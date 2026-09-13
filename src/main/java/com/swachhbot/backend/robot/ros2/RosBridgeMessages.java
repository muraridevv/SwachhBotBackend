package com.swachhbot.backend.robot.ros2;

import com.fasterxml.jackson.annotation.JsonInclude;

public final class RosBridgeMessages {
    private RosBridgeMessages() {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Advertise(String op, String topic, String type) {
        public Advertise(String topic, String type) { this("advertise", topic, type); }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Publish(String op, String topic, Object msg) {
        public Publish(String topic, Object msg) { this("publish", topic, msg); }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Subscribe(String op, String topic, String type) {
        public Subscribe(String topic, String type) { this("subscribe", topic, type); }
    }

    // ROS Message Types
    public record Twist(Vector3 linear, Vector3 angular) {}
    public record Vector3(double x, double y, double z) {}
    
    public record PoseStamped(Header header, Pose pose) {}
    public record Header(long seq, Time stamp, String frame_id) {}
    public record Time(long secs, long nsecs) {}
    public record Pose(Point position, Quaternion orientation) {}
    public record Point(double x, double y, double z) {}
    public record Quaternion(double x, double y, double z, double w) {}
}
