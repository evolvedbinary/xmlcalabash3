package com.xmlcalabash.steps.validation

import com.thaiopensource.relaxng.parse.IllegalSchemaException
import com.thaiopensource.relaxng.pattern.FeasibleTransform
import com.thaiopensource.relaxng.pattern.IdTypeMap
import com.thaiopensource.relaxng.pattern.IdTypeMapBuilder
import com.thaiopensource.relaxng.pattern.Pattern
import com.thaiopensource.relaxng.pattern.SchemaBuilderImpl
import com.thaiopensource.relaxng.pattern.SchemaPatternBuilder
import com.thaiopensource.util.PropertyMap
import com.thaiopensource.validate.AbstractSchema
import com.thaiopensource.validate.CombineSchema
import com.thaiopensource.validate.IncorrectSchemaException
import com.thaiopensource.validate.ResolverFactory
import com.thaiopensource.validate.Schema
import com.thaiopensource.validate.ValidateProperty
import com.thaiopensource.validate.Validator
import com.thaiopensource.validate.prop.rng.RngProperty
import com.thaiopensource.validate.prop.wrap.WrapProperty
import com.thaiopensource.validate.rng.impl.FeasibleIdTypeMapSchema
import com.thaiopensource.validate.rng.impl.IdTypeMapSchema
import com.thaiopensource.validate.rng.impl.PatternSchema
import com.thaiopensource.validate.rng.impl.SchemaReaderImpl
import org.relaxng.datatype.helpers.DatatypeLibraryLoader
import javax.xml.transform.sax.SAXSource

abstract class RelaxNGSchemaReader(): SchemaReaderImpl() {
    private val supportedPropertyIds = arrayOf(
        ValidateProperty.XML_READER_CREATOR,
        ValidateProperty.ERROR_HANDLER,
        ValidateProperty.ENTITY_RESOLVER,
        ValidateProperty.URI_RESOLVER,
        ValidateProperty.RESOLVER,
        RngProperty.DATATYPE_LIBRARY_FACTORY,
        RngProperty.CHECK_ID_IDREF,
        RngProperty.FEASIBLE,
        WrapProperty.ATTRIBUTE_OWNER
    )

    override fun createSchema(source: SAXSource, properties: PropertyMap): Schema {
        val sbp = SchemaPatternBuilder()
        val resolver = ResolverFactory.createResolver(properties)
        val errHandler = properties.get(ValidateProperty.ERROR_HANDLER)
        val dlf = properties.get(RngProperty.DATATYPE_LIBRARY_FACTORY) ?: DatatypeLibraryLoader()
        try {
            val start = SchemaBuilderImpl.parse(createParseable(source, resolver, errHandler, properties),
                errHandler, dlf, sbp, properties.contains(WrapProperty.ATTRIBUTE_OWNER))
            return wrapPattern2(start, sbp, properties)
        } catch (ex: IllegalSchemaException) {
            throw IncorrectSchemaException()
        }
    }

    private fun wrapPattern2(initialStart: Pattern, spb: SchemaPatternBuilder, initialProperties: PropertyMap): SchemaWrapper {
        var start = initialStart

        if (initialProperties.contains(RngProperty.FEASIBLE)) {
            // Use a feasible transform
            start = FeasibleTransform.transform(spb, start)
        }

        // Get properties for supported IDs
        val properties = AbstractSchema.filterProperties(initialProperties, supportedPropertyIds)
        var schema: Schema = PatternSchema(spb, start, properties)

        var idTypeMap: IdTypeMap? = null
        if (spb.hasIdTypes() && properties.contains(RngProperty.CHECK_ID_IDREF)) {
            // Check ID/IDREF
            val eh = properties.get(ValidateProperty.ERROR_HANDLER)
            idTypeMap = IdTypeMapBuilder(eh, start).getIdTypeMap()
            if (idTypeMap == null) {
                throw IncorrectSchemaException()
            }
            val idSchema = if (properties.contains(RngProperty.FEASIBLE)) {
                FeasibleIdTypeMapSchema(idTypeMap, properties)
            } else {
                IdTypeMapSchema(idTypeMap, properties)
            }

            schema = CombineSchema(schema, idSchema, properties)
        }

        //Wrap the schema
        return SchemaWrapper(schema, start, idTypeMap)
    }

    class SchemaWrapper(val schema: Schema, val start: Pattern, val idTypeMap: IdTypeMap?): Schema {
        override fun createValidator(properties: PropertyMap?): Validator? {
            return schema.createValidator(properties)
        }

        override fun getProperties(): PropertyMap? {
            return schema.getProperties()
        }
    }
}