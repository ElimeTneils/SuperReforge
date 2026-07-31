package com.mutuo.superreforge.definition;

import java.util.List;

/** 一次收集所有定义错误，方便整合包作者一次修改而不是反复 reload。 */
public record ValidationReport(List<String> errors) {
    public ValidationReport {
        errors = List.copyOf(errors);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    /** 在发布快照前把完整错误列表合并为一个异常。 */
    public void throwIfInvalid() {
        if (!isValid()) {
            throw new DefinitionValidationException(String.join(System.lineSeparator(), errors));
        }
    }
}
