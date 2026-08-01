package com.mutuo.superreforge.reforge;

import java.util.Optional;

/** 报价要么成功携带 quote，要么只携带明确失败原因。 */
public record ReforgeQuoteResult(Optional<ReforgeQuote> quote, Optional<ReforgeFailure> failure) {
    public static ReforgeQuoteResult success(ReforgeQuote quote) {
        return new ReforgeQuoteResult(Optional.of(quote), Optional.empty());
    }

    public static ReforgeQuoteResult failure(ReforgeFailure failure) {
        return new ReforgeQuoteResult(Optional.empty(), Optional.of(failure));
    }
}
