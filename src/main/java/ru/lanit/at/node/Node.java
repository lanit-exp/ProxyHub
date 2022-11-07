package ru.lanit.at.node;

import com.fasterxml.jackson.annotation.JsonFilter;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
@JsonFilter("nodeFilter")
public class Node {

    private String address;
    private String sessionId;
    private long lastActivity;

    public Node(String address) {
        this.address = address;
        this.sessionId = "";
        this.lastActivity = 0;
    }
}
