"""Validation tests for the Phase 2 shared JSON contracts."""

from backend.app.models.schemas import (
    IntentRequest,
    UiElement,
    UiTree,
    WorkflowDefinition,
    WorkflowStep,
)


def test_ui_tree_contract_validates() -> None:
    payload = {
        "screen": "food_app",
        "elements": [
            {
                "id": "node_1",
                "role": "edit_text",
                "text": "Search",
                "content_description": "Search food",
                "resource_id": "search_box",
                "clickable": True,
                "enabled": True,
            }
        ],
    }

    ui_tree = UiTree.model_validate(payload)
    assert ui_tree.screen == "food_app"
    assert ui_tree.elements[0].role == "edit_text"


def test_workflow_contract_validates() -> None:
    flow = WorkflowDefinition(
        flow_id="order_food",
        intent="ORDER_FOOD",
        parameters=["item", "quantity"],
        steps=[
            WorkflowStep(action="search", target={"role": "edit_text"}, value="{{item}}"),
            WorkflowStep(action="tap", target={"text": "Add to Cart"}),
        ],
    )

    assert flow.flow_id == "order_food"
    assert flow.parameters == ["item", "quantity"]
    assert flow.steps[1].target["text"] == "Add to Cart"


def test_intent_request_contract_validates() -> None:
    req = IntentRequest(text="Order 2 pizzas")
    assert req.text == "Order 2 pizzas"


def test_ui_element_contract_validates() -> None:
    element = UiElement(
        id="node_2",
        role="button",
        text="Add to Cart",
        content_description="",
        resource_id="add_cart",
        clickable=True,
        enabled=True,
    )
    assert element.id == "node_2"
    assert element.text == "Add to Cart"
