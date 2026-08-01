package com.mutuo.superreforge.definition;

/** reload 定义未通过聚合校验时抛出的明确异常。 */
public final class DefinitionValidationException extends IllegalArgumentException {
    public DefinitionValidationException(String message) {
        super(message);
    }
}
