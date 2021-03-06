package de.deepamehta.accesscontrol.migrations;

import de.deepamehta.config.ConfigService;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;

import java.util.List;
import java.util.logging.Logger;



/**
 * Adds "Login enabled" config topic to each username.
 * Runs only in UPDATE mode.
 * <p>
 * Note: when CLEAN_INSTALLing the admin user already got its config topics
 * as the Config service is already in charge.
 * <p>
 * Part of DM 4.7
 */
public class Migration11 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private ConfigService configService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        List<Topic> usernames = dm4.getTopicsByType("dm4.accesscontrol.username");
        logger.info("########## Adding \"dm4.accesscontrol.login_enabled\" config topic to " + usernames.size() +
            " usernames");
        for (Topic username : usernames) {
            configService.createConfigTopic("dm4.accesscontrol.login_enabled", username);
        }
    }
}
