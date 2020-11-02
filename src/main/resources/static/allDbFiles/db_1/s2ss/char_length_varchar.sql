ALTER FUNCTION [s2ss].[char_length_varchar](@expression varchar(max))
RETURNS INT
AS
BEGIN
  RETURN CASE WHEN len(replace(@expression, ' ', '.')) = 0
	          THEN 1
		  ELSE len(replace(@expression, ' ', '.'))
 	   END
END