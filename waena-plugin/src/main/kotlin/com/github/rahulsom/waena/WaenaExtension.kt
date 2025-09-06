package com.github.rahulsom.waena

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.gradle.api.Project
import org.gradle.api.provider.Property

open class WaenaExtension(project: Project) {
  enum class License(val license: String, val url: String) {
    Apache2("The Apache License, Version 2.0", "https://opensource.org/licenses/Apache-2.0"),
    BSD3("The 3-Clause BSD License", "https://opensource.org/licenses/BSD-3-Clause"),
    BSD2("The 2-Clause BSD License", "https://opensource.org/licenses/BSD-2-Clause"),
    GPL2("GNU General Public License version 2", "https://opensource.org/licenses/GPL-2.0"),
    GPL3("GNU General Public License version 3", "https://opensource.org/licenses/GPL-3.0"),
    LGPL2("GNU Library General Public License version 2", "https://opensource.org/licenses/LGPL-2.0"),
    LGPL21("GNU Lesser General Public License version 2.1", "https://opensource.org/licenses/LGPL-2.1"),
    LGPL3("GNU Lesser General Public License version 3", "https://opensource.org/licenses/LGPL-3.0"),
    MIT("The MIT License", "https://opensource.org/licenses/MIT"),
    MPL2("Mozilla Public License 2.0 (MPL-2.0)", "https://opensource.org/licenses/MPL-2.0"),
    CDDL1("Common Development and Distribution License 1.0", "https://opensource.org/licenses/CDDL-1.0"),
    EPL2("Eclipse Public License version 2.0", "https://opensource.org/licenses/EPL-2.0");
  }

  enum class PublishMode {
    /**
     * Publish to Maven Central via Sonatype Portal
     */
    Central,
  }

  val license: Property<License> = project.objects
    .property(License::class.java)
    .convention(License.Apache2)
  val publishMode: Property<PublishMode> = project.objects
    .property(PublishMode::class.java)
    .convention(PublishMode.Central)

  @Throws(JsonProcessingException::class)
  fun toJson(): String? {
    return ObjectMapper().writeValueAsString(
      mapOf<String, Any>(
        "license" to license.get(),
        "publishMode" to publishMode.get()
      )
    )
  }
}
