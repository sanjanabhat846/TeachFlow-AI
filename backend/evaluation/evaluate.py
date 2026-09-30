"""Run the deterministic TeachFlow AI backend evaluation dataset."""

from __future__ import annotations

import json
import shutil
import sys
import tempfile
from pathlib import Path
from typing import Any

from fastapi.testclient import TestClient

from backend.app.flow import flow_store
from backend.app.main import create_app
from backend.app.semantic.ui_matcher import match_ui_element, recover_target


ROOT = Path(__file__).resolve().parents[2]
DATASET_PATH = Path(__file__).with_name("dataset.json")


def _record(
    results: list[dict[str, Any]],
    case_id: str,
    passed: bool,
    actual: Any,
    expected: Any,
    known_gap: bool = False,
) -> None:
    results.append(
        {
            "id": case_id,
            "passed": passed,
            "actual": actual,
            "expected": expected,
            "known_gap": known_gap,
        }
    )


def _evaluate_intent(client: TestClient, dataset: dict) -> list[dict[str, Any]]:
    results = []
    for case in dataset["intent_extraction"]:
        response = client.post("/intent/classify", json={"text": case["text"]})
        actual = response.json().get("intent") if response.status_code == 200 else response.status_code
        _record(results, case["id"], actual == case["expected"], actual, case["expected"])
    return results


def _evaluate_parameters(client: TestClient, dataset: dict) -> list[dict[str, Any]]:
    results = []
    for case in dataset["parameter_extraction"]:
        response = client.post("/intent/parameters", json={"text": case["text"]})
        actual = response.json() if response.status_code == 200 else {"status": response.status_code}
        if case.get("expected_invalid"):
            passed = response.status_code == 422 or (
                response.status_code == 200 and actual.get("parameters") == {}
            )
            expected: Any = "reject as invalid (422 or empty parameters)"
        else:
            expected = case["expected"]
            passed = actual == expected
        _record(results, case["id"], passed, actual, expected, case.get("known_gap", False))
    return results


def _evaluate_workflows(client: TestClient, dataset: dict) -> list[dict[str, Any]]:
    results = []
    for case in dataset["workflow_matching"]:
        if case["kind"] == "match":
            response = client.post("/flows/match", json={"text": case["text"]})
            actual = response.json().get("flow_id") if response.status_code == 200 else response.status_code
            expected = case["expected_flow_id"]
        elif case["kind"] == "bind":
            response = client.post(
                "/execution/plan",
                json={
                    "intent": case["intent"],
                    "flow_id": case["flow_id"],
                    "parameters": case["parameters"],
                },
            )
            actual = response.json().get("actions") if response.status_code == 200 else response.status_code
            expected = case["expected_actions"]
        else:
            response = client.post(
                "/execution/plan",
                json={"intent": case["intent"], "flow_id": case["flow_id"]},
            )
            actual = response.status_code
            expected = case["expected_status"]
        _record(results, case["id"], actual == expected, actual, expected)
    return results


def _evaluate_semantic(dataset: dict) -> list[dict[str, Any]]:
    results = []
    for case in dataset["semantic_ui_matching"]:
        actual = match_ui_element(case["target"], case["ui_tree"])
        expected = {
            "matched": case["expected_matched"],
            "node_id": case["expected_node_id"],
        }
        observed = {"matched": actual["matched"], "node_id": actual["node_id"]}
        _record(results, case["id"], observed == expected, observed, expected)
    return results


def _evaluate_recovery(dataset: dict) -> list[dict[str, Any]]:
    results = []
    for case in dataset["recovery"]:
        actual = recover_target(case["target"], case["ui_tree"])
        expected = {
            "matched": case["expected_matched"],
            "requires_clarification": case["expected_clarification"],
        }
        observed = {
            "matched": actual["matched"],
            "requires_clarification": actual["requires_clarification"],
        }
        if "expected_node_id" in case:
            expected["node_id"] = case["expected_node_id"]
            observed["node_id"] = actual["node_id"]
        _record(results, case["id"], observed == expected, observed, expected)
    return results


def _evaluate_safety(client: TestClient, dataset: dict) -> list[dict[str, Any]]:
    results = []
    for case in dataset["safety"]:
        response = client.post("/safety/check", json={"actions": case["actions"]})
        actual = (
            response.json().get("requires_approval")
            if response.status_code == 200
            else response.status_code
        )
        expected = case["expected_approval"]
        _record(results, case["id"], actual == expected, actual, expected)
    return results


def _evaluate_end_to_end(client: TestClient, dataset: dict) -> list[dict[str, Any]]:
    results = []
    for case in dataset["end_to_end"]:
        try:
            intent = client.post("/intent/classify", json={"text": case["command"]})
            parameters = client.post("/intent/parameters", json={"text": case["command"]})
            flow_match = client.post("/flows/match", json={"text": case["command"]})
            if any(response.status_code != 200 for response in (intent, parameters, flow_match)):
                raise RuntimeError("Intent, parameter, or workflow-match API request failed")

            extracted = parameters.json()["parameters"]
            flow_id = flow_match.json()["flow_id"]
            plan = client.post(
                "/execution/plan",
                json={
                    "intent": intent.json()["intent"],
                    "flow_id": flow_id,
                    "parameters": extracted,
                },
            )
            if plan.status_code != 200:
                raise RuntimeError(f"Execution plan returned HTTP {plan.status_code}")

            tap_action = next(action for action in plan.json()["actions"] if action["action"] == "tap")
            ui_match = client.post(
                "/ui/match",
                json={
                    "target": tap_action["target"],
                    "ui_tree": {
                        "screen": "food_app",
                        "elements": [
                            {
                                "id": case["expected_ui_node_id"],
                                "role": "button",
                                "text": "Add",
                                "content_description": "Add products to cart",
                                "context": "Cart actions",
                                "clickable": True,
                                "enabled": True,
                            }
                        ],
                    },
                },
            )
            safety = client.post("/safety/check", json={"actions": plan.json()["actions"]})
            if ui_match.status_code != 200 or safety.status_code != 200:
                raise RuntimeError("Semantic-match or safety API request failed")

            approval_required = safety.json()["requires_approval"]
            result_response = None
            if not approval_required and ui_match.json()["matched"]:
                result_response = client.post(
                    "/execution/result",
                    json={"success": True, "step": 1, "message": "Backend contract evaluation"},
                )

            actual = {
                "intent": intent.json()["intent"],
                "parameters": extracted,
                "flow_id": flow_id,
                "ui_matched": ui_match.json()["matched"],
                "ui_node_id": ui_match.json()["node_id"],
                "safety_requires_approval": approval_required,
                "execution_result_status": result_response.status_code if result_response else None,
            }
            expected = {
                "intent": case["expected_intent"],
                "parameters": case["expected_parameters"],
                "flow_id": case["expected_flow_id"],
                "ui_matched": True,
                "ui_node_id": case["expected_ui_node_id"],
                "safety_requires_approval": case["expected_safety_approval"],
                "execution_result_status": case["expected_result_status"],
            }
            passed = all(actual[key] == value for key, value in expected.items())
            _record(results, case["id"], passed, actual, expected)
        except Exception as error:
            _record(results, case["id"], False, str(error), "complete backend API sequence")
    return results


def _evaluate_android_static(dataset: dict) -> list[dict[str, Any]]:
    results = []
    for case in dataset["android_static_checks"]:
        source = (ROOT / case["file"]).read_text(encoding="utf-8")
        required_order = (
            "TeachFlowApiClient.checkSafety(step)",
            "onRequestApproval(",
            "while (!approvedState && !cancelState)",
            "if (cancelState)",
            "ActionExecutor.executeStep(",
        )
        positions = [source.find(token) for token in required_order]
        passed = all(position >= 0 for position in positions) and positions == sorted(positions)
        actual = "safety check, approval wait/cancel branch, then action execution" if passed else positions
        _record(results, case["id"], passed, actual, "approval gate precedes accessibility action")
    return results


def _print_metric(name: str, results: list[dict[str, Any]]) -> bool:
    passed = sum(result["passed"] for result in results)
    print(f"{name}: {passed}/{len(results)} cases passed (exact expected-outcome checks)")
    for result in results:
        if not result["passed"]:
            label = "KNOWN GAP" if result["known_gap"] else "FAIL"
            print(f"  {label} {result['id']}: expected {result['expected']}; observed {result['actual']}")
    return passed == len(results)


def main() -> int:
    dataset = json.loads(DATASET_PATH.read_text(encoding="utf-8"))
    original_store_path = flow_store.STORE_PATH
    all_passed = True

    with tempfile.TemporaryDirectory(prefix="teachflow-eval-") as temp_dir:
        flow_store.STORE_PATH = Path(temp_dir) / "workflows.json"
        client = TestClient(create_app())
        try:
            seeded = client.post("/flows/learn", json=dataset["workflow"])
            if seeded.status_code != 200:
                raise RuntimeError(f"Could not seed evaluation workflow: HTTP {seeded.status_code}")

            groups = [
                ("Intent extraction (backend API)", _evaluate_intent(client, dataset)),
                ("Parameter extraction (backend API)", _evaluate_parameters(client, dataset)),
                ("Workflow matching/binding (backend API)", _evaluate_workflows(client, dataset)),
                ("Semantic UI matching (backend module)", _evaluate_semantic(dataset)),
                ("Recovery/clarification (backend module)", _evaluate_recovery(dataset)),
                ("Safety detection (backend API)", _evaluate_safety(client, dataset)),
                ("End-to-end logical sequence (backend API)", _evaluate_end_to_end(client, dataset)),
            ]
            print(f"TeachFlow AI evaluation dataset v{dataset['version']}")
            print("Backend evaluation uses deterministic local inputs and a temporary workflow store.")
            for name, results in groups:
                all_passed = _print_metric(name, results) and all_passed

            static_results = _evaluate_android_static(dataset)
            _print_metric("Android/static source check (not runtime)", static_results)

            wrapper_jar = ROOT / "android/TeachFlow/gradle/wrapper/gradle-wrapper.jar"
            system_gradle = shutil.which("gradle")
            if wrapper_jar.exists() or system_gradle:
                print("Android build: available but not run by this backend evaluator.")
            else:
                print("Android build: NOT RUN (Gradle wrapper JAR and system Gradle are unavailable).")
            print("Real-device validation: NOT RUN; no device interaction is performed by this evaluator.")
            print("Execution-result endpoint validates its request contract; it does not execute or persist Android actions.")
        finally:
            client.close()
            flow_store.STORE_PATH = original_store_path

    return 0 if all_passed else 1


if __name__ == "__main__":
    sys.exit(main())