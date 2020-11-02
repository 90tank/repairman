
CREATE PROCEDURE po_cpd_get_agency
   @ps_group char(2),
   @ps_agt_code char(5),
   @ps_agy_code varchar(5)
AS
   DECLARE
      @agy_code char(5),
      @a char(5)
   SELECT @a = substring(@ps_agy_code, 0, 1)
   IF (s2ss.char_length_varchar(@ps_agy_code) < 5)
      BEGIN
         SELECT @agy_code = min(a.agy_code)
         FROM db_ebiz.dbo.tcpd_iarb  AS a
         WHERE a.agt_code = @ps_agt_code
      END
   ELSE
      BEGIN
         SELECT @agy_code = min(@ps_agy_code)
      END

   /*
   *
   *      DD - find agency list
   *
   */
   BEGIN
      SELECT DISTINCT a.agy_code AS agy_code
      FROM db_ebiz.dbo.tcpd_iarb  AS a
      WHERE 1 = 1 AND a.dist_agycode = @agy_code
      ORDER BY a.agy_code
   END
   RETURN
GO

