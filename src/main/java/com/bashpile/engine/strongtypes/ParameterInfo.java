package com.bashpile.engine.strongtypes;

import javax.annotation.Nonnull;

public record ParameterInfo(@Nonnull String name, @Nonnull Type type, @Nonnull String defaultValue) {
}
