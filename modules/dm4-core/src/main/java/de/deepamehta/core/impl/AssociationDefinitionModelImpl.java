package de.deepamehta.core.impl;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.TopicDeletionModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.model.ViewConfigurationModel;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;



/**
 * Definition of an association between 2 topic types -- part of DeepaMehta's type system,
 * like an association in a class diagram. Used to represent both, aggregations and compositions.
 * ### FIXDOC: also assoc types have assoc defs
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
class AssociationDefinitionModelImpl extends AssociationModelImpl implements AssociationDefinitionModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String parentCardinalityUri;
    private String childCardinalityUri;

    private ViewConfigurationModelImpl viewConfig;     // is never null

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * ### TODO: add include-in-label parameter?
     *
     * @param   customAssocTypeUri      if null no custom association type will be set.
     */
    AssociationDefinitionModelImpl(AssociationModelImpl assoc, String parentCardinalityUri, String childCardinalityUri,
                                                                                ViewConfigurationModelImpl viewConfig) {
        super(assoc);
        this.parentCardinalityUri = parentCardinalityUri;
        this.childCardinalityUri  = childCardinalityUri;
        this.viewConfig = viewConfig != null ? viewConfig : mf.newViewConfigurationModel();
        // ### TODO: why null check? Compare to TypeModelImpl constructor
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String getAssocDefUri() {
        String customAssocTypeUri = getCustomAssocTypeUriOrNull();
        return getChildTypeUri() + (customAssocTypeUri !=null ? "#" + customAssocTypeUri : "");
    }

    @Override
    public String getCustomAssocTypeUri() {
        TopicModel customAssocType = getCustomAssocType();
        return customAssocType != null ? customAssocType.getUri() : null;
    }

    /**
     * The type to be used to create an association instance based on this association definition.
     */
    @Override
    public String getInstanceLevelAssocTypeUri() {
        String customAssocTypeUri = getCustomAssocTypeUri();
        return customAssocTypeUri !=null ? customAssocTypeUri : defaultInstanceLevelAssocTypeUri();
    }

    @Override
    public String getParentTypeUri() {
        return ((TopicRoleModel) getRoleModel("dm4.core.parent_type")).getTopicUri();
    }

    @Override
    public String getChildTypeUri() {
        return ((TopicRoleModel) getRoleModel("dm4.core.child_type")).getTopicUri();
    }

    @Override
    public String getParentCardinalityUri() {
        return parentCardinalityUri;
    }

    @Override
    public String getChildCardinalityUri() {
        return childCardinalityUri;
    }

    @Override
    public ViewConfigurationModelImpl getViewConfigModel() {
        return viewConfig;
    }

    // ---

    @Override
    public void setParentCardinalityUri(String parentCardinalityUri) {
        this.parentCardinalityUri = parentCardinalityUri;
    }

    @Override
    public void setChildCardinalityUri(String childCardinalityUri) {
        this.childCardinalityUri = childCardinalityUri;
    }

    @Override
    public void setViewConfigModel(ViewConfigurationModel viewConfig) {
        this.viewConfig = (ViewConfigurationModelImpl) viewConfig;
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            return super.toJSON()
                .put("parent_cardinality_uri", parentCardinalityUri)
                .put("child_cardinality_uri", childCardinalityUri)
                .put("view_config_topics", viewConfig.toJSONArray());
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // ---

    @Override
    public String toString() {
        return "\n    association definition (" + super.toString() +
            ",\n        parent cardinality=\"" + parentCardinalityUri +
            "\",\n        child cardinality=\"" + childCardinalityUri +
            "\",\n        " + viewConfig + ")\n";
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    @Override
    String className() {
        return "association definition";
    }

    @Override
    AssociationDefinition instantiate() {
        return new AssociationDefinitionImpl(this, pl);
    }



    // === Core Internal Hooks ===

    @Override
    void postUpdate(DeepaMehtaObjectModel newModel, DeepaMehtaObjectModel oldModel) {
        super.postUpdate(newModel, oldModel);
        //
        updateCardinality((AssociationDefinitionModel) newModel);
        //
        // rehash
        boolean changeCustomAssocType = customAssocTypeChange((AssociationDefinitionModel) newModel,
            (AssociationDefinitionModel) oldModel);
        if (changeCustomAssocType) {
            logger.info("### Changed custom association type URI from \"" +
                ((AssociationDefinitionModelImpl) oldModel).getCustomAssocTypeUri() + "\" -> \"" +
                ((AssociationDefinitionModelImpl) newModel).getCustomAssocTypeUriOrNull() + "\"");
            getParentType().rehashAssocDef(newModel.getId());
        }
    }



    // === Update (memory + DB) ===

    void updateParentCardinalityUri(String parentCardinalityUri) {
        setParentCardinalityUri(parentCardinalityUri);                      // update memory
        pl.typeStorage.storeParentCardinalityUri(id, parentCardinalityUri); // update DB
    }

    void updateChildCardinalityUri(String childCardinalityUri) {
        setChildCardinalityUri(childCardinalityUri);                        // update memory
        pl.typeStorage.storeChildCardinalityUri(id, childCardinalityUri);   // update DB
    }



    // ===

    /**
     * ### TODO: make private
     *
     * @return  <code>null</code> if this assoc def's custom assoc type model is null or represents a deletion ref.
     *          Otherwise returns the custom assoc type URI.
     */
    String getCustomAssocTypeUriOrNull() {
        return getCustomAssocType() instanceof TopicDeletionModel ? null : getCustomAssocTypeUri();
    }

    // ---

    TypeModelImpl getParentType() {
        return pl.typeStorage.getType(getParentTypeUri());
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Update ===

    private void updateCardinality(AssociationDefinitionModel newModel) {
        updateParentCardinality(newModel.getParentCardinalityUri());
        updateChildCardinality(newModel.getChildCardinalityUri());
    }

    // ---

    private void updateParentCardinality(String newParentCardinalityUri) {
        // abort if no update is requested
        if (newParentCardinalityUri == null) {
            return;
        }
        //
        String parentCardinalityUri = getParentCardinalityUri();
        if (!parentCardinalityUri.equals(newParentCardinalityUri)) {
            logger.info("### Changing parent cardinality URI from \"" + parentCardinalityUri + "\" -> \"" +
                newParentCardinalityUri + "\"");
            updateParentCardinalityUri(newParentCardinalityUri);
        }
    }

    private void updateChildCardinality(String newChildCardinalityUri) {
        // abort if no update is requested
        if (newChildCardinalityUri == null) {
            return;
        }
        //
        String childCardinalityUri = getChildCardinalityUri();
        if (!childCardinalityUri.equals(newChildCardinalityUri)) {
            logger.info("### Changing child cardinality URI from \"" + childCardinalityUri + "\" -> \"" +
                newChildCardinalityUri + "\"");
            updateChildCardinalityUri(newChildCardinalityUri);
        }
    }



    // ====

    private boolean customAssocTypeChange(AssociationDefinitionModel newModel, AssociationDefinitionModel oldModel) {
        String oldUri = oldModel.getCustomAssocTypeUri();   // null if no assoc type is set
        String newUri = ((AssociationDefinitionModelImpl) newModel).getCustomAssocTypeUriOrNull();  // null if del ref
        if (newUri != null) {
            // new value is neither a deletion ref nor null, compare it to old value (which may be null)
            return !newUri.equals(oldUri);
        } else {
            // compare old value to null if new value is a deletion ref or null
            // ### FIXME: must differentiate "no change requested" (= null) and "remove current assignment" (= del ref)?
            return oldUri != null;
        }
    }

    private RelatedTopicModel getCustomAssocType() {
        RelatedTopicModel customAssocType = getChildTopicsModel().getTopicOrNull(
            "dm4.core.assoc_type#dm4.core.custom_assoc_type");
        // Note: we can't do this sanity check because a type model would not even deserialize.
        // The type model JSON constructor repeatedly calls addAssocDef() which hashes by assoc def URI. ### still true?
        /* if (customAssocType instanceof TopicDeletionModel) {
            throw new RuntimeException("Tried to get an assoc def's custom assoc type when it is a deletion " +
                "reference (" + this + ")");
        } */
        return customAssocType;
    }

    private String defaultInstanceLevelAssocTypeUri() {
        if (typeUri.equals("dm4.core.aggregation_def")) {
            return "dm4.core.aggregation";
        } else if (typeUri.equals("dm4.core.composition_def")) {
            return "dm4.core.composition";
        } else {
            throw new RuntimeException("Unexpected association type URI: \"" + typeUri + "\"");
        }
    }
}
