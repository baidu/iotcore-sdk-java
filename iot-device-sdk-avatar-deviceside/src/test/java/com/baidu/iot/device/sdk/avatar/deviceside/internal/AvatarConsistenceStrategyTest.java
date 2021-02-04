package com.baidu.iot.device.sdk.avatar.deviceside.internal;

import org.junit.Assert;
import org.junit.Test;

/**
 * @Author zhangxiao18
 * @Date 2020/10/21
 */
public class AvatarConsistenceStrategyTest {

    @Test
    public void testReport() {
        AvatarConsistenceStrategy avatarConsistenceStrategy = new AvatarConsistenceStrategy(1000);
        avatarConsistenceStrategy.force(true);
        Assert.assertTrue(avatarConsistenceStrategy.isConsistent());

        avatarConsistenceStrategy.recordReportResult(false);
        Assert.assertFalse(avatarConsistenceStrategy.isConsistent());

        avatarConsistenceStrategy.force(true);
        Assert.assertTrue(avatarConsistenceStrategy.isConsistent());

        avatarConsistenceStrategy.recordReportResult(true);
        Assert.assertTrue(avatarConsistenceStrategy.isConsistent());
    }

    @Test
    public void testLongTimeInterval() throws InterruptedException {
        AvatarConsistenceStrategy avatarConsistenceStrategy = new AvatarConsistenceStrategy(100);
        avatarConsistenceStrategy.force(true);
        Assert.assertTrue(avatarConsistenceStrategy.isConsistent());

        Thread.sleep(300);
        Assert.assertFalse(avatarConsistenceStrategy.isConsistent());

        avatarConsistenceStrategy.force(true);
        avatarConsistenceStrategy.recordReportResult(true);
        Assert.assertTrue(avatarConsistenceStrategy.isConsistent());
        Thread.sleep(300);
        Assert.assertFalse(avatarConsistenceStrategy.isConsistent());

        avatarConsistenceStrategy.force(true);
        avatarConsistenceStrategy.recordLatestDesiredVersion(10);
        Assert.assertTrue(avatarConsistenceStrategy.isConsistent());
        Thread.sleep(300);
        Assert.assertFalse(avatarConsistenceStrategy.isConsistent());
    }

    @Test
    public void testReportVersion() {
        AvatarConsistenceStrategy avatarConsistenceStrategy = new AvatarConsistenceStrategy(1000);
        avatarConsistenceStrategy.force(true);
        Assert.assertTrue(avatarConsistenceStrategy.isConsistent());

        avatarConsistenceStrategy.recordLatestDesiredVersion(10);
        Assert.assertTrue(avatarConsistenceStrategy.isConsistent());

        avatarConsistenceStrategy.recordLatestDesiredVersion(11);
        Assert.assertTrue(avatarConsistenceStrategy.isConsistent());

        avatarConsistenceStrategy.recordLatestDesiredVersion(13);
        Assert.assertFalse(avatarConsistenceStrategy.isConsistent());
    }

    @Test
    public void testForceSet() {
        AvatarConsistenceStrategy avatarConsistenceStrategy = new AvatarConsistenceStrategy(1000);
        avatarConsistenceStrategy.force(true);
        Assert.assertTrue(avatarConsistenceStrategy.isConsistent());

        avatarConsistenceStrategy.force(false);
        Assert.assertFalse(avatarConsistenceStrategy.isConsistent());
    }

}
