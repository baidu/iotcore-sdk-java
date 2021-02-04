/*
 * Copyright (c) 2020 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.iot.device.sdk.avatar.common.internal;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.regex.Pattern;

/**
 * @Author zhangxiao18
 * @Date 2020/9/23
 */
@Getter
@EqualsAndHashCode
@ToString
public class Topic {

    public enum TopicType {
        GET("$iot/%s/shadow/get", Pattern.compile("\\$iot/.+/shadow/get")),
        GET_REPLY("$iot/%s/shadow/get/reply", Pattern.compile("\\$iot/.+/shadow/get/reply")),
        UPDATE_REPORTED("$iot/%s/shadow/update/reported", Pattern.compile("\\$iot/.+/shadow/update/reported")),
        UPDATE_REPORTED_REPLY("$iot/%s/shadow/update/reported/reply",
                Pattern.compile("\\$iot/.+/shadow/update/reported/reply")),
        UPDATE_DESIRED("$iot/%s/shadow/update/desired", Pattern.compile("\\$iot/.+/shadow/update/desired")),
        UPDATE_DESIRED_REPLY("$iot/%s/shadow/update/desired/reply",
                Pattern.compile("\\$iot/.+/shadow/update/desired/reply")),
        DESIRED_DELTA("$iot/%s/shadow/delta/desired", Pattern.compile("\\$iot/.+/shadow/delta/desired")),
        REPORTED_DELTA("$iot/%s/shadow/delta/reported", Pattern.compile("\\$iot/.+/shadow/delta/reported"));

        private String format;
        private Pattern regex;

        TopicType(String format, Pattern regex) {
            this.format = format;
            this.regex = regex;
        }

        String create(AvatarId avatarId) {
            return String.format(format, avatarId.getId());
        }

        static TopicType of(String topic) {
            for (TopicType topicType : TopicType.values()) {
                if (topicType.regex.matcher(topic).matches()) {
                    return topicType;
                }
            }
            return null;
        }
    }

    public Topic(String topicFilter) {
        this.qos = 1;
        this.topicFilter = topicFilter;
        this.topicType = TopicType.of(topicFilter);
    }

    private final int qos;

    private final String topicFilter;

    @Getter
    private final TopicType topicType;

    public static Topic genGetTopic(AvatarId avatarId) {
        return new Topic(TopicType.GET.create(avatarId));
    }

    public static Topic genGetReplyTopic(AvatarId avatarId) {
        return new Topic(TopicType.GET_REPLY.create(avatarId));
    }

    public static Topic genUpdateReportedTopic(AvatarId avatarId) {
        return new Topic(TopicType.UPDATE_REPORTED.create(avatarId));
    }

    public static Topic genUpdateReportedReplyTopic(AvatarId avatarId) {
        return new Topic(TopicType.UPDATE_REPORTED_REPLY.create(avatarId));
    }

    public static Topic genUpdateDesiredTopic(AvatarId avatarId) {
        return new Topic(TopicType.UPDATE_DESIRED.create(avatarId));
    }

    public static Topic genUpdateDesiredReplyTopic(AvatarId avatarId) {
        return new Topic(TopicType.UPDATE_DESIRED_REPLY.create(avatarId));
    }

    public static Topic genDesiredDeltaTopic(AvatarId avatarId) {
        return new Topic(TopicType.DESIRED_DELTA.create(avatarId));
    }

    public static Topic genReportedDeltaTopic(AvatarId avatarId) {
        return new Topic(TopicType.REPORTED_DELTA.create(avatarId));
    }
}
