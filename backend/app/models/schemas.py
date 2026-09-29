"""Pydantic schemas for TeachFlow AI backend contracts."""

from __future__ import annotations

from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class UiElement(BaseModel):
    """Accessibility element from the Android app."""

    model_config = ConfigDict(extra="forbid")

    id: str
    role: str = ""
    text: str = ""
    content_description: str = ""
    resource_id: str = ""
    clickable: bool = False
    enabled: bool = True


class UiTree(BaseModel):
    """Collection of accessibility elements belonging to one screen."""

    model_config = ConfigDict(extra="forbid")

    screen: str = ""
    elements: list[UiElement] = Field(default_factory=list)


class WorkflowStep(BaseModel):
    """A semantic step inside a learned workflow."""

    model_config = ConfigDict(extra="forbid")

    action: str
    target: dict[str, Any] | None = None
    value: str | int | None = None


class WorkflowDefinition(BaseModel):
    """Reusable workflow learned from a user demonstration."""

    model_config = ConfigDict(extra="forbid")

    flow_id: str
    intent: str
    parameters: list[str] = Field(default_factory=list)
    steps: list[WorkflowStep] = Field(default_factory=list)


class ExecutionResult(BaseModel):
    """Result of a single execution step."""

    model_config = ConfigDict(extra="forbid")

    success: bool
    step: int
    message: str | None = None
    error: str | None = None


class SafetyCheckResult(BaseModel):
    """Whether a workflow requires human approval before execution."""

    model_config = ConfigDict(extra="forbid")

    requires_approval: bool = False
    reason: str | None = None


class IntentRequest(BaseModel):
    """Request payload for intent classification."""

    model_config = ConfigDict(extra="forbid")

    text: str
    context: str | None = None


class IntentResponse(BaseModel):
    """Response payload for intent classification."""

    model_config = ConfigDict(extra="forbid")

    intent: str
    confidence: float = 0.0
    source: str = "rule_based"


class ParameterExtractionRequest(BaseModel):
    """Request payload for parameter extraction."""

    model_config = ConfigDict(extra="forbid")

    text: str
    intent: str | None = None


class ParameterExtractionResponse(BaseModel):
    """Response payload for parameter extraction."""

    model_config = ConfigDict(extra="forbid")

    intent: str
    parameters: dict[str, Any] = Field(default_factory=dict)


class LearnWorkflowRequest(BaseModel):
    """Request payload for learning a workflow from a demonstration."""

    model_config = ConfigDict(extra="forbid")

    flow_id: str
    intent: str
    parameters: list[str] = Field(default_factory=list)
    steps: list[WorkflowStep] = Field(default_factory=list)


class LearnWorkflowResponse(BaseModel):
    """Response payload after saving a learned workflow."""

    model_config = ConfigDict(extra="forbid")

    success: bool
    flow_id: str
    message: str


class FlowMatchRequest(BaseModel):
    """Natural-language command for workflow lookup."""

    model_config = ConfigDict(extra="forbid")

    text: str


class FlowMatchResponse(BaseModel):
    """Best matching workflow result."""

    model_config = ConfigDict(extra="forbid")

    flow_id: str
    confidence: float = 0.0


class ExecutionPlanRequest(BaseModel):
    """Request payload for workflow execution planning."""

    model_config = ConfigDict(extra="forbid")

    intent: str
    parameters: dict[str, Any] = Field(default_factory=dict)
    flow_id: str | None = None


class ExecutionPlanResponse(BaseModel):
    """Ordered semantic actions Android should execute."""

    model_config = ConfigDict(extra="forbid")

    actions: list[dict[str, Any]] = Field(default_factory=list)


class UiTreeRequest(BaseModel):
    """Screen payload sent from Android to backend."""

    model_config = ConfigDict(extra="forbid")

    ui_tree: UiTree
