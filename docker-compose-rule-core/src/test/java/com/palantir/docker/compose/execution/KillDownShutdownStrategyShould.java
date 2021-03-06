/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.docker.compose.execution;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.palantir.docker.compose.configuration.ShutdownStrategy;
import org.junit.Test;
import org.mockito.InOrder;

public class KillDownShutdownStrategyShould {

    @Test
    public void call_kill_on_stop() throws Exception {
        DockerCompose dockerCompose = mock(DockerCompose.class);

        ShutdownStrategy.KILL_DOWN.stop(dockerCompose);

        InOrder inOrder = inOrder(dockerCompose);
        inOrder.verify(dockerCompose).kill();
        verifyNoMoreInteractions(dockerCompose);
    }

    @Test
    public void call_down_on_shutdown() throws Exception {
        DockerCompose dockerCompose = mock(DockerCompose.class);
        Docker docker = mock(Docker.class);

        ShutdownStrategy.KILL_DOWN.shutdown(dockerCompose, docker);

        InOrder inOrder = inOrder(dockerCompose);
        inOrder.verify(dockerCompose).down();
        verifyNoMoreInteractions(dockerCompose, docker);
    }
}
