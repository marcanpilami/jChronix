Run on Karaf
##################

feature:install http http-whiteboard webconsole scr

install -s mvn:javax.annotation/javax.annotation-api/1.3
install -s mvn:javax.validation/validation-api/1.1.0.Final
install -s mvn:javax.ws.rs/javax.ws.rs-api/2.1

install -s mvn:aopalliance/aopalliance/1.0

install -s mvn:org.javassist/javassist

install -s mvn:org.glassfish.hk2/osgi-resource-locator/1.0.1
install -s mvn:org.glassfish.hk2.external/javax.inject
install -s mvn:org.glassfish.hk2.external/aopalliance-repackaged
install -s mvn:org.glassfish.hk2/hk2-api/2.5.0-b43
install -s mvn:org.glassfish.hk2/hk2-utils/2.5.0-b43
install -s mvn:org.glassfish.hk2/hk2-locator/2.5.0-b43
install -s mvn:org.glassfish.jersey.inject/jersey-hk2/2.26-b09
install -s mvn:org.glassfish.jersey.core/jersey-server/2.26-b09
install -s mvn:org.glassfish.jersey.core/jersey-client/2.26-b09
install -s mvn:org.glassfish.jersey.core/jersey-common/2.26-b09
install -s mvn:org.glassfish.jersey.containers/jersey-container-servlet-core/2.26-b09
install -s mvn:org.glassfish.jersey.containers/jersey-container-servlet/2.26-b09


Then copy the project jar inside KARAF_ROOT/deploy. 