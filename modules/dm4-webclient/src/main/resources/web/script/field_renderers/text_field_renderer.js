function TextFieldRenderer(field_model) {
    this.field_model = field_model
}

TextFieldRenderer.prototype.render_field = function(parent_element) {
    dm4c.render.field_label(this.field_model, parent_element)
    parent_element.append(js.render_text(this.field_model.value))
}

TextFieldRenderer.prototype.render_form_element = function(parent_element) {
    // Input field: a jQuery object
    // Text area:   a jQuery object
    // Combo box:   a GUIToolkit Combobox object
    var form_element = render_form_element(this.field_model)
    //
    return function() {
        if (form_element instanceof jQuery) {
            return $.trim(form_element.val())
        } else {
            // form_element is a Combobox
            var selection = form_element.get_selection() // either a menu item (object) or the text entered (string)
            if (typeof(selection) == "object") {
                // user selected existing topic
                return dm4c.REF_PREFIX + selection.value
            } else {
                // user entered new value
                return selection
            }
        }
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function render_form_element(field_model) {
        // error check
        if (!field_model.rows) {
            throw "TextFieldRendererError: field \"" + field_model.label + "\" has no \"rows\" setting"
        }
        //
        if (field_model.rows == 1) {
            switch (field_model.assoc_def && field_model.assoc_def.assoc_type_uri) {
            case undefined:
                // Note: for non-composite topics the field's assoc_def is undefined.
                // We treat this like a composition here.
            case "dm4.core.composition_def":
                return render_input()
            case "dm4.core.aggregation_def":
                return render_combobox()
            default:
                throw "TextFieldRendererError: \"" + field_model.assoc_def.assoc_type_uri +
                    "\" is an unexpected assoc type URI"
            }
        } else {
            return render_textarea()
        }

        function render_input() {
            var input = dm4c.render.input(field_model)
            if (field_model.autocomplete_indexes) {
                var page_renderer = dm4c.get_page_renderer(field_model.toplevel_topic)
                input.keyup(page_renderer.autocomplete)
                input.blur(page_renderer.lost_focus)
                input.attr({autocomplete: "off"})
            }
            //
            render(input)
            return input
        }

        function render_textarea() {
            var textarea = $("<textarea>").attr("rows", field_model.rows).text(field_model.value)
            //
            render(textarea)
            return textarea
        }

        function render_combobox() {
            // fetch all instances
            var topics = dm4c.restc.get_topics(field_model.topic_type.uri, false, true).items  // fetch_composite=false,
            //                                                                                 // sort=true
            var combobox = create_combobox()
            //
            render(combobox.dom)
            return combobox

            function create_combobox() {
                var combobox = dm4c.ui.combobox()
                // add items
                for (var i in topics) {
                    combobox.add_item({label: topics[i].value, value: topics[i].id})
                }
                // select item
                combobox.select_by_label(field_model.value)
                //
                return combobox
            }
        }

        function render(form_element) {
            parent_element.append(form_element)
        }
    }
}
