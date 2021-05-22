package org.oxymores.chronix.web.staticloader;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardResource;

/**
 * This class merely exists to give a static resource mapping configuration to the OSGi HTTP whiteboard, nothing more.
 */
@Component(service = StaticResourceServer.class)
@HttpWhiteboardResource(pattern = "/static/*", prefix = "/reactbuild")
public class StaticResourceServer
{
}
