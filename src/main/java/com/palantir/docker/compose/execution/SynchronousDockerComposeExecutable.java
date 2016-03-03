/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 *
 * THIS SOFTWARE CONTAINS PROPRIETARY AND CONFIDENTIAL INFORMATION OWNED BY PALANTIR TECHNOLOGIES INC.
 * UNAUTHORIZED DISCLOSURE TO ANY THIRD PARTY IS STRICTLY PROHIBITED
 *
 * For good and valuable consideration, the receipt and adequacy of which is acknowledged by Palantir and recipient
 * of this file ("Recipient"), the parties agree as follows:
 *
 * This file is being provided subject to the non-disclosure terms by and between Palantir and the Recipient.
 *
 * Palantir solely shall own and hereby retains all rights, title and interest in and to this software (including,
 * without limitation, all patent, copyright, trademark, trade secret and other intellectual property rights) and
 * all copies, modifications and derivative works thereof.  Recipient shall and hereby does irrevocably transfer and
 * assign to Palantir all right, title and interest it may have in the foregoing to Palantir and Palantir hereby
 * accepts such transfer. In using this software, Recipient acknowledges that no ownership rights are being conveyed
 * to Recipient.  This software shall only be used in conjunction with properly licensed Palantir products or
 * services.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.palantir.docker.compose.execution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static com.google.common.base.Throwables.propagate;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.stream.Collectors.joining;

public class SynchronousDockerComposeExecutable {
    public static final int HOURS_TO_WAIT_FOR_STD_OUT_TO_CLOSE = 12;
    public static final int MINUTES_TO_WAIT_AFTER_STD_OUT_CLOSES = 1;
    private final DockerComposeExecutable dockerComposeExecutable;
    private final Consumer<String> logConsumer;

    public SynchronousDockerComposeExecutable(DockerComposeExecutable dockerComposeExecutable,
            Consumer<String> logConsumer) {
        this.dockerComposeExecutable = dockerComposeExecutable;
        this.logConsumer = logConsumer;
    }

    public ProcessResult run(String... commands) throws IOException, InterruptedException {
        Process process = dockerComposeExecutable.execute(commands);

        Future<String> outputProcessing = newSingleThreadExecutor()
                .submit(() -> processOutputFrom(process));

        String output = waitForResultFrom(outputProcessing);

        process.waitFor(MINUTES_TO_WAIT_AFTER_STD_OUT_CLOSES, TimeUnit.MINUTES);

        return new ProcessResult(process.exitValue(), output);
    }

    private String processOutputFrom(Process process) {
        return asReader(process.getInputStream()).lines()
                    .peek(logConsumer)
                    .collect(joining(System.lineSeparator()));
    }

    private String waitForResultFrom(Future<String> outputProcessing) {
        try {
            return outputProcessing.get(HOURS_TO_WAIT_FOR_STD_OUT_TO_CLOSE, TimeUnit.HOURS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw propagate(e);
        }
    }

    private BufferedReader asReader(InputStream inputStream) {return new BufferedReader(new InputStreamReader(inputStream));}
}
