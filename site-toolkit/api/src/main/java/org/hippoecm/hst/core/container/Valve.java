package org.hippoecm.hst.core.container;

import org.hippoecm.hst.core.request.HstRequestContext;

public interface Valve
{
    public void invoke(ValveContext context) throws ContainerException;

    /**
     * Initialize the valve before using in a pipeline.
     */
    public void initialize() throws ContainerException;

}