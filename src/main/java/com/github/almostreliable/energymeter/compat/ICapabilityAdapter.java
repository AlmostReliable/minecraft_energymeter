package com.github.almostreliable.energymeter.compat;


public interface ICapabilityAdapter<T> {

    /**
     * Checks if the provided capability is a compatible peripheral capability.
     *
     * @param cap The capability to check.
     * @return True if the capability is compatible, false otherwise.
     */
//    boolean isCapability(Capability<?> cap);

    /**
     * Gets the capability instance as a lazy optional.
     *
     * @return The capability instance as a lazy optional.
     */
//    LazyOptional<T> getLazyAdapter();
}
