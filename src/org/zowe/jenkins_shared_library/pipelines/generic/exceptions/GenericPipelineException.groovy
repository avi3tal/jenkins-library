/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2019
 */

package org.zowe.jenkins_shared_library.pipelines.generic.exceptions

/**
 * An exception that can be thrown from the {@link org.zowe.jenkins_shared_library.pipelines.generic.GenericPipeline} class
 */
class GenericPipelineException extends Exception {
    /**
     * Construct the exception.
     * @param message The exception message.
     */
    GenericPipelineException(String message) {
        super(message)
    }
}
