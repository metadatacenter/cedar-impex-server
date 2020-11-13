<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="text" omit-xml-declaration="yes" indent="no"/>
  <xsl:strip-space elements="*"/>

  <xsl:template match="/SubmissionStatus">

    <xsl:text>&#xA;</xsl:text>
    <xsl:if test="@submission_id">
      <xsl:text>Submission NCBI ID: </xsl:text><xsl:value-of select="@submission_id"/><xsl:text>&#xA;</xsl:text>
    </xsl:if>
    <xsl:text>Status: </xsl:text><xsl:value-of select="@status"/><xsl:text>&#xA;</xsl:text>
    <xsl:if test="Message">
      <xsl:text>Message: </xsl:text><xsl:value-of select="Message"/><xsl:text>&#xA;</xsl:text>
    </xsl:if>
    <xsl:if test="Tracking/SubmissionLocation">
      <xsl:text>Location: </xsl:text><xsl:value-of select="Tracking/SubmissionLocation"/><xsl:text>&#xA;</xsl:text>
    </xsl:if>

    <xsl:for-each select="Action">
      <xsl:text> - Action ID: </xsl:text><xsl:value-of select="@action_id"/><xsl:text>&#xA;</xsl:text>
      <xsl:text>   Status: </xsl:text><xsl:value-of select="@status"/><xsl:text>&#xA;</xsl:text>
      <xsl:text>   Target_DB: </xsl:text><xsl:value-of select="@target_db"/><xsl:text>&#xA;</xsl:text>
      <xsl:if test="Response/Message">
        <xsl:text>   Message: </xsl:text><xsl:value-of select="Response/Message"/><xsl:text>&#xA;</xsl:text>
      </xsl:if>
    </xsl:for-each>

  </xsl:template>

</xsl:stylesheet>