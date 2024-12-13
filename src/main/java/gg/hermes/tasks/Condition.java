package gg.hermes.tasks;

import java.util.Map;

public class Condition implements ICondition
{
    private String rule;
    private Map<String, Object> data;

    private Condition() {}

    public Condition(String rule, Map<String, Object> data) {
        this.rule = rule;
        this.data = data;
    }

    @Override
    public String getRule() {
        return rule;
    }

    @Override
    public Map<String, Object> getData() {
        return data;
    }
}
