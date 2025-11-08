package com.prtlabs.rlalc.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public record ProgramId(@JsonValue String uuid) {}