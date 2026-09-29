"""FastAPI application entry point for the TeachFlow AI backend."""

from __future__ import annotations

from fastapi import FastAPI, HTTPException, Response

from backend.app.executor.execution_planner import build_execution_plan
from backend.app.executor.safety_checker import check_safety
from backend.app.flow.flow_matcher import match_flow
from backend.app.flow.flow_store import delete_flow, get_flow, list_flows, save_flow
from backend.app.flow.flow_synthesizer import synthesize_flow
from backend.app.intent.classifier import classify_intent
from backend.app.intent.parameter_extractor import extract_parameters
from backend.app.models.schemas import (
    ExecutionPlanRequest,
    ExecutionPlanResponse,
    ExecutionResult,
    FlowMatchRequest,
    FlowMatchResponse,
    IntentRequest,
    IntentResponse,
    LearnWorkflowRequest,
    LearnWorkflowResponse,
    ParameterExtractionRequest,
    ParameterExtractionResponse,
    SafetyCheckRequest,
    SafetyCheckResult,
    UiMatchRequest,
    UiMatchResponse,
    UiRecoveryResponse,
    WorkflowDefinition,
    WorkflowSynthesisRequest,
)
from backend.app.semantic.ui_matcher import match_ui_element, recover_target


def create_app() -> FastAPI:
    """Create the API application and register backend capability routes."""
    app = FastAPI(title="TeachFlow AI Backend", version="0.1.0")

    @app.get("/health")
    def health() -> dict[str, str]:
        return {"status": "ok"}

    @app.post("/intent/classify", response_model=IntentResponse)
    def classify(request: IntentRequest) -> IntentResponse:
        intent = classify_intent(request.text)
        return IntentResponse(
            intent=intent,
            confidence=1.0 if intent != "UNKNOWN" else 0.0,
        )

    @app.post("/intent/parameters", response_model=ParameterExtractionResponse)
    def parameters(request: ParameterExtractionRequest) -> dict:
        return extract_parameters(request.text)

    @app.post("/flows/synthesize", response_model=WorkflowDefinition)
    def synthesize(request: WorkflowSynthesisRequest) -> dict:
        workflow = synthesize_flow(request.demonstration)
        return save_flow(workflow)

    @app.post("/flows/learn", response_model=LearnWorkflowResponse)
    def learn(request: LearnWorkflowRequest) -> LearnWorkflowResponse:
        save_flow(request.model_dump(exclude_none=True))
        return LearnWorkflowResponse(
            success=True,
            flow_id=request.flow_id,
            message="Workflow saved successfully",
        )

    @app.get("/flows", response_model=list[WorkflowDefinition])
    def flows() -> list[dict]:
        return list_flows()

    @app.get("/flows/{flow_id}", response_model=WorkflowDefinition)
    def flow(flow_id: str) -> dict:
        stored_flow = get_flow(flow_id)
        if stored_flow is None:
            raise HTTPException(status_code=404, detail="Workflow not found")
        return stored_flow

    @app.delete("/flows/{flow_id}")
    def remove_flow(flow_id: str) -> dict[str, object]:
        deleted = delete_flow(flow_id)
        if not deleted:
            raise HTTPException(status_code=404, detail="Workflow not found")
        return {"deleted": True, "flow_id": flow_id}

    @app.post("/flows/match", response_model=FlowMatchResponse)
    def match(request: FlowMatchRequest) -> dict:
        return match_flow(request.text)

    @app.post("/execution/plan", response_model=ExecutionPlanResponse)
    def plan(request: ExecutionPlanRequest) -> ExecutionPlanResponse:
        if request.flow_id:
            selected_flow = get_flow(request.flow_id)
            if selected_flow is None:
                raise HTTPException(status_code=404, detail="Workflow not found")
        else:
            candidates = [
                stored_flow
                for stored_flow in list_flows()
                if stored_flow.get("intent") == request.intent
            ]
            if not candidates:
                raise HTTPException(status_code=404, detail="No workflow found for intent")
            if len(candidates) > 1:
                raise HTTPException(
                    status_code=409,
                    detail="flow_id is required when multiple workflows match the intent",
                )
            selected_flow = candidates[0]

        if selected_flow.get("intent") != request.intent:
            raise HTTPException(status_code=422, detail="Workflow intent does not match request")

        try:
            actions = build_execution_plan(selected_flow, request.parameters)
        except ValueError as error:
            raise HTTPException(status_code=422, detail=str(error)) from error
        return ExecutionPlanResponse(actions=actions)

    @app.post("/execution/result", status_code=204)
    def execution_result(result: ExecutionResult) -> Response:
        return Response(status_code=204)

    @app.post("/safety/check", response_model=SafetyCheckResult)
    def safety(request: SafetyCheckRequest) -> dict:
        return check_safety(request.actions)

    @app.post("/ui/match", response_model=UiMatchResponse)
    def ui_match(request: UiMatchRequest) -> dict:
        return match_ui_element(request.target, request.ui_tree.model_dump())

    @app.post("/ui/recover", response_model=UiRecoveryResponse)
    def ui_recover(request: UiMatchRequest) -> dict:
        return recover_target(request.target, request.ui_tree.model_dump())

    return app


app = create_app()


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("backend.app.main:app", host="0.0.0.0", port=8000)
