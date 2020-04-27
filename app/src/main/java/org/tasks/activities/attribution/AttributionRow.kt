package org.tasks.activities.attribution

class AttributionRow {
    val isHeader: Boolean
    val license: String?
    val copyrightHolder: String?
    val libraries: String?

    internal constructor(license: String?) {
        this.license = license
        isHeader = true
        copyrightHolder = null
        libraries = null
    }

    internal constructor(copyrightHolder: String?, libraries: String?) {
        this.copyrightHolder = copyrightHolder
        this.libraries = libraries
        isHeader = false
        license = null
    }
}