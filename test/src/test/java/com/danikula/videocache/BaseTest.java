package com.danikula.videocache;

import com.danikula.videocache.test.BuildConfig;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public abstract class BaseTest {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
    }

}
