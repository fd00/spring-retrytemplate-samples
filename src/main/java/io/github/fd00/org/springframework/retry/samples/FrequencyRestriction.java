package io.github.fd00.org.springframework.retry.samples;

import lombok.Getter;

public class FrequencyRestriction {
    private int frequency;

    @Getter
    private int current;

    public FrequencyRestriction(int frequency) {
        this.frequency = frequency;
        this.current = 0;
    }

    public boolean isLimited() {
        return (frequency <= current++);
    }
}
