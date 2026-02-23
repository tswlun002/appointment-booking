package capitec.branch.appointment.kafka.domain;

public record KeyValue<K, V>(K key, V value) {}