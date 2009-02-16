package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.core.component.HstComponentWindow;

public interface ValveContext
{
    public void invokeNext() throws ContainerException;
    
    public ServletRequest getServletRequest();

    public ServletResponse getServletResponse();
    
    public void setRootComponentWindow(HstComponentWindow rootComponentWindow);
    
    public HstComponentWindow getRootComponentWindow();
    
}
