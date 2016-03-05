/**
 * This package contains everything needed to create advanced <strong>agent</strong> plugins.<br>
 * Agents are threads running independently of the engine (they can be launched even if the engine is not running) which can communicate to
 * the engine or another agent through a provided messaging service, and can otherwise do pretty much whatever they want.<br>
 * The messaging service is very low-level, as it directly exposes JMS interfaces.<br>
 * Agents are responsible for their own lifecycle - most notably, they must start themselves. Recommendation is to do so through the
 * presence of an OSGI configuration file named with the ID of the plugin. To do that, simply decorate your main plugin class with
 * <code>@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)</code> <br>
 * An example provided with the base distribution is the shell agent - an agent that awaits shell commands to run on a message queue and
 * returns the result.
 */
@org.osgi.annotation.versioning.Version("1.0.0")
package org.oxymores.chronix.api.agent;
