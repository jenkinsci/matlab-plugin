package com.mathworks.ci;

import java.io.Serial;

/**
 * Copyright 2020-2024 The MathWorks, Inc.
 */

public class MatlabNotFoundError extends Error {

    @Serial
    private static final long serialVersionUID = 7918595075502022644L;

    MatlabNotFoundError(String errorMessage) {
        super(errorMessage);
    }

}
