package com.gulvisha.backend.organization;

import java.util.List;

/**
 * JSON structure for the organization.site_content jsonb column.
 * Each field is optional — the frontend falls back to sensible defaults.
 *
 * @param heroEyebrow        small label above the hero title
 * @param heroTitle          main headline (supports {brandName} placeholder)
 * @param heroSubheading     text under the title
 * @param aboutHeading       heading for the about section
 * @param aboutCapabilities  bullet list for the about panel
 * @param industries         list of industry names
 * @param processHeading     heading for the process section
 * @param processSteps       ordered process steps
 * @param quoteHeading       heading for the quote/contact section
 * @param quoteSubheading    subheading under the quote heading
 * @param quoteDescription   text above the form
 */
public record SiteContent(
        String heroEyebrow,
        String heroTitle,
        String heroSubheading,
        String aboutHeading,
        List<String> aboutCapabilities,
        List<String> industries,
        String processHeading,
        List<ProcessStep> processSteps,
        String quoteHeading,
        String quoteSubheading,
        String quoteDescription
) {
    public record ProcessStep(String step, String title, String description) {
    }
}
