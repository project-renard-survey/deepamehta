/**
 * A TopicmapRenderer implementation that uses a canvas to draw the topics and associations.
 * Holds a topicmap viewmodel and binds it to the view.
 */
function CanvasRenderer() {

    // ------------------------------------------------------------------------------------------------ Constructor Code

    js.extend(this, TopicmapRenderer)

    // Viewmodel
    var topicmap                    // the topicmap currently rendered (a TopicmapViewmodel).
                                    // Initialized by display_topicmap().
    var viewmodel_customizers = []  // registered 3rd-party viewmodel customizer instances

    // View
    var canvas = new CanvasView()

    this.dom = canvas.dom

    // ------------------------------------------------------------------------------------------------------ Public API



    // === TopicmapRenderer Implementation ===

    this.get_info = function() {
        return {
            uri: "dm4.webclient.default_topicmap_renderer",
            name: "Topicmap"
        }
    }

    // ---

    this.load_topicmap = function(topicmap_id, config) {
        config.customizers = viewmodel_customizers
        return new TopicmapViewmodel(topicmap_id, config, dm4c.restc)
    }

    this.display_topicmap = function(topicmap_viewmodel, no_history_update) {

        topicmap = topicmap_viewmodel
        track_images()

        /**
         * Defers "display_topicmap" until all topicmap images are loaded.
         */
        function track_images() {
            var image_tracker = dm4c.create_image_tracker(display_topicmap)
            // add type icons
            topicmap.iterate_topics(function(topic) {
                if (topic.visibility) {
                    // Note: accessing the type icon requires accessing the type. However the user might have no
                    // explicit READ permission for the type. We must enforce the *implicit* READ permission.
                    dm4c.enforce_implicit_topic_type_read_permission(topic)
                    //
                    image_tracker.add_image(dm4c.get_type_icon(topic.type_uri))
                }
            })
            // add background image
            if (topicmap.background_image) {
                image_tracker.add_image(topicmap.background_image)
            }
            //
            image_tracker.check()
        }

        function display_topicmap() {

            canvas.display_topicmap(topicmap)
            restore_selection()

            function restore_selection() {
                var id = topicmap.selected_object_id
                if (id != -1) {
                    if (topicmap.is_topic_selected) {
                        dm4c.do_select_topic(id, no_history_update)
                    } else {
                        dm4c.do_select_association(id, no_history_update)
                    }
                } else {
                    dm4c.do_reset_selection(no_history_update)
                }
            }
        }
    }

    // ---

    /**
     * Adds a topic to the canvas. If the topic is already on the canvas it is not added again. ### FIXDOC
     *
     * @param   topic       A Topic object with optional "x", "y" properties.
     *                      (Any object with "id", "type_uri", and "value" properties is suitable.)
     * @param   do_select   Optional: if true, the topic is selected.
     */
    this.show_topic = function(topic, do_select) {
        canvas.init_topic_position(topic)
        // update viewmodel
        var topic_viewmodel = topicmap.add_topic(topic, topic.x, topic.y)
        if (do_select) {
            topicmap.set_topic_selection(topic.id)
        }
        // update view
        if (topic_viewmodel) {
            canvas.show_topic(topic_viewmodel)
        }
        if (do_select) {
            canvas.set_topic_selection(topic.id)
        }
        //
        return topic
    }

    /**
     * @param   assoc       An Association object.
     *                      (Any object with "id", "type_uri", "role_1", "role_2" properties is suitable.)
     * @param   do_select   Optional: if true, the association is selected.
     */
    this.show_association = function(assoc, do_select) {
        // update viewmodel
        var assoc_viewmodel = topicmap.add_association(assoc)
        if (do_select) {
            topicmap.set_association_selection(assoc.id)
        }
        // update view
        if (assoc_viewmodel) {
            canvas.show_association(assoc_viewmodel)
        }
        if (do_select) {
            canvas.set_association_selection(assoc.id)
        }
    }

    // ---

    /**
     * Removes a topic from the canvas (model) and optionally refreshes the canvas (view). ### FIXDOC
     * If the topic is not present on the canvas nothing is performed.
     */
    this.hide_topic = function(topic_id) {
        // update viewmodel
        topicmap.hide_topic(topic_id)
        // update view
        canvas.remove_topic(topic_id)
    }

    /**
     * Removes an association from the canvas (model) and optionally refreshes the canvas (view). ### FIXDOC
     * If the association is not present on the canvas nothing is performed.
     */
    this.hide_association = function(assoc_id) {
        // update viewmodel
        topicmap.hide_association(assoc_id)
        // update view
        canvas.remove_association(assoc_id)
    }

    // ---

    /**
     * Updates a topic. If the topic is not on the canvas nothing is performed. ### FIXDOC
     *
     * @param   topic       A Topic object.
     */
    this.update_topic = function(topic) {
        // update viewmodel
        var topic_viewmodel = for_all_topicmaps("update_topic", topic)
        // update view
        if (topic_viewmodel) {
            canvas.update_topic(topic_viewmodel)
        }
    }

    /**
     * Updates an association. If the association is not on the canvas nothing is performed. ### FIXDOC
     *
     * @param   assoc       An Association object.
     */
    this.update_association = function(assoc) {
        // update viewmodel
        var assoc_viewmodel = for_all_topicmaps("update_association", assoc)
        // update view
        if (assoc_viewmodel) {
            canvas.update_association(assoc_viewmodel)
        }
    }

    // ---

    this.delete_topic = function(topic_id) {
        // update viewmodel
        for_all_topicmaps("delete_topic", topic_id)
        // update view
        canvas.remove_topic(topic_id)
    }

    this.delete_association = function(assoc_id) {
        // update viewmodel
        for_all_topicmaps("delete_association", assoc_id)
        // update view
        canvas.remove_association(assoc_id)
    }

    // ---

    this.update_topic_type = function(topic_type) {
        // Note: viewmodel update not required
        //
        // update view
        canvas.update_topic_type(topic_type)
    }

    this.update_association_type = function(assoc_type) {
        // Note: viewmodel update not required
        //
        // update view
        canvas.update_association_type(assoc_type)
    }

    // ---

    this.is_topic_visible = function(topic_id) {
        var topic = topicmap.get_topic(topic_id)
        return topic && topic.visibility
    }

    // ---

    this.select_topic = function(topic_id) {
        // fetch from DB
        var topic = dm4c.fetch_topic(topic_id, true, true)  // include_childs=true, include_assoc_childs=true
        // update viewmodel
        topicmap.set_topic_selection(topic_id)
        // update view
        canvas.set_topic_selection(topic_id)
        //
        return {select: topic, display: topic}
    }

    this.select_association = function(assoc_id) {
        // fetch from DB
        var assoc = dm4c.fetch_association(assoc_id, true)  // include_childs=true
        // update viewmodel
        topicmap.set_association_selection(assoc_id)
        // update view
        canvas.set_association_selection(assoc_id)
        //
        return assoc
    }

    this.reset_selection = function() {
        // update viewmodel
        topicmap.reset_selection()
        // update view
        canvas.reset_selection()
    }

    // ---

    this.scroll_topic_to_center = function(topic_id) {
        canvas.scroll_to_center(topic_id)
    }

    this.begin_association = function(topic_id, x, y) {
        canvas.begin_association(topic_id, x, y)
    }



    // === Grid Positioning ===

    this.start_grid_positioning = function() {
        canvas.start_grid_positioning()
    }

    this.stop_grid_positioning = function() {
        canvas.stop_grid_positioning()
    }



    // === Left SplitPanel Component Implementation ===

    /**
     * Called in 3 situations:
     * 1) Initialization: the topicmap renderer is added to the split panel.
     * 2) The user resizes the main window.
     * 3) The user moves the split panel's slider.
     *
     * @param   size    an object with "width" and "height" properties.
     */
    this.resize = function(size) {
        canvas.resize(size)
    }



    // === End of interface implementations ===

    /**
     * Registers a view customizer. Callable by 3rd-party plugins.
     *
     * There are 2 flavors of view customizers:
     *     CANVAS_FLAVOR - draws a topic on canvas
     *     DOM_FLAVOR    - creates a per-topic DOM
     *
     * A view customizer supports these hooks:
     *     "on_update_topic"
     *     "on_update_view_properties"
     *     "on_move_topic"
     *     "on_mousedown"
     *     "draw_topic"
     *     "topic_dom"
     *          Customizes the per-topic DOM. A TopicView is passed, with the default DOM in its "dom" property.
     *          The default DOM consists of a div of class "topic". The customizer can append childs to it.
     *
     *          The default DOM sets up event listeners for performing topic standard operations: selection, moving,
     *          receiving association in progress, edit on double-click, invoke context menu on right-click.
     *     "topic_dom_appendix"
     *     "topic_dom_draggable_handle"
     *
     * All hooks are optional.
     *
     * A view customizer can suppress the invocation of the corresponding default hook by returning a falsish value.
     *
     * @param   view_customizer     The view customizer (a constructor function).
     */
    this.register_view_customizer = function(view_customizer) {
        canvas.register_view_customizer(view_customizer)
    }

    /**
     * @param   viewmodel_customizer    The viewmodel customizer (a constructor function).
     */
    this.register_viewmodel_customizer = function(viewmodel_customizer) {
        viewmodel_customizers.push(new viewmodel_customizer())
    }

    // ---

    this.get_topic_associations = function(topic_id) {
        return topicmap.get_topic_associations(topic_id)
    }

    // ----------------------------------------------------------------------------------------------- Private Functions



    /**
     * Iterates through all topicmaps and calls the given function with the given argument on them.
     * Returns the function call's return value for the topicmap that is currently displayed.
     *
     * ### TODO: updating *all* topicmaps should not be the responsibility of the topicmap renderer.
     */
    function for_all_topicmaps(topicmap_func, arg) {
        var return_value
        dm4c.get_plugin("de.deepamehta.topicmaps").iterate_topicmaps(function(_topicmap) {
            var ret = _topicmap[topicmap_func](arg)
            if (topicmap.get_id() == _topicmap.get_id()) {
                return_value = ret
            }
        })
        return return_value
    }
}
