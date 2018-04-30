package org.opendaylight.genius.testutils;

import org.mockito.Mockito;
import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.realOrException;

public class TestItmProvider {

    public static TestItmProvider newInstance() {
        return Mockito.mock(TestItmProvider.class, realOrException());
    }
}
