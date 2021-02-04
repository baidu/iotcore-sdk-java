package com.baidu.iot.device.sdk.avatar.common;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * @Author zhangxiao18
 * @Date 2020/10/18
 */
public class PropertyKeyTest {

    @Test
    public void testCreate() {
        String key= "a.b.c";
        PropertyKey propertyKey = new PropertyKey(key);
        Assert.assertEquals(key, propertyKey.toString());
        Assert.assertEquals(3, propertyKey.getEntries().size());
        Assert.assertEquals("a", propertyKey.getEntries().get(0));
    }

    @Test
    public void testFindChildren() {
        PropertyKey parent= new PropertyKey("a.b");

        PropertyKey child1 = new PropertyKey("a.b.c");
        PropertyKey child2 = new PropertyKey("a.b.d");
        PropertyKey child3 = new PropertyKey("a.b.c.d");
        PropertyKey child4 = new PropertyKey("a.c");
        PropertyKey child5 = new PropertyKey("a");
        PropertyKey child6 = new PropertyKey("a.b");

        Set<PropertyKey> result = parent.findChildren(Sets.newHashSet(child1, child2, child3, child4, child5, child6));

        Assert.assertTrue(result.contains(child1));
        Assert.assertTrue(result.contains(child2));
        Assert.assertTrue(result.contains(child3));

        Assert.assertFalse(result.contains(child4));
        Assert.assertFalse(result.contains(child5));
        Assert.assertFalse(result.contains(child6));

    }

}
