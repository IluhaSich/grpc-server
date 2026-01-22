package com.example.grpc_server;

import com.example.events_contract.events.DelicacyAnalyzedEvent;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsListener {

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "q.grpc.delicacy.analytics", durable = "true"),
                    exchange = @Exchange(name = "delicacy-analytics-fanout", type = "fanout")
            )
    )
    public void onAnalytics(DelicacyAnalyzedEvent event) {
        System.out.println(
                "gRPC-SERVER received analytics for delicacy " + event.delicacyId()
        );
    }
}
