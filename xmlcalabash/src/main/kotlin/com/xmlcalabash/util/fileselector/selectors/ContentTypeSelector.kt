package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.fileselector.FSFile

class ContentTypeSelector(val stepConfig: XProcStepConfiguration, val types: List<MediaType>): Selector {
    override fun selects(file: FSFile): Boolean {
        val mtype = MediaType.parse(stepConfig.documentManager.mimetypesFileTypeMap.getContentType(file.path))
        val match = mtype.matchingMediaType(types)
        return match != null && match.inclusive
    }
}