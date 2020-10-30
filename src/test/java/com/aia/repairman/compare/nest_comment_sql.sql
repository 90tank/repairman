/*a*/
/*a
/*bsss/*haha*/aaa*/
*/
select
select
--test zy
create proc po_com_col_full
(@doc_id char(10), @comp_no char(3), @pol_no char(10), @li_letter_gen_seq integer output)
as
/*  Created by Nickie Poon on 15 Aug 97
    Function :  Update common columns for POSAG letter
    Called by : All POS main proc
    Updated by Georgia Chau on 2 Jan 1998
    1.  Extract address using Payor's record instead of Owner's record, if Owner's address not found
    Updated by Georgia Chau on 14 Jan 1998
    1.  Add ':' after CC for both Chi / Eng 
    Updated by Georgia on 4 Feb 98  : 
    1.  If Tel. no. of agency phone no. is  null or <= '', 
        suppress 'Tel :'' in both e_cc_to / c_cc_to
    Updated by Georgia Chau on 25 Aug 98, suppress extra space for Chinese Character 
    Updated by KH on 26 Feb 03, Agent Opt-Out    
*/
select * from ttest
select
--testzy2
select * from ttest
----------------------------------------------------------------------
-- MPBicor Migration				
--
-- Description : As the old Bicor will be migrated to MPBicor, 
--		the following items in stored proccedures 
--		will be modified to meet the new requirement.
--
-- Modified items: 
--  
-- 1) 	The cycle date information will be changed to obtain from 
--	db_print.tcycle_date table.
-- 	   
--	  e.g.  select @cyc_date=current_cycle_date 
--		from db_print..tcycle_date where cycle_id = "LFCM"
--
-- 2)	Change the database name from db_cor to db_print of bicor 
--	system and transaction tables
--
--	  e.g. 	transaction tables
-- 			tdata_pool, tdata_pool_dtl, tjob_log, etc.
--
--		system tables
--			tcontent, thdr_ftr, tcycle_date, etc.
--
-- 3)   Calling common stored procedure - po_get_letrseq to get the letter 
--	generate sequence no. (letter_gen_seq) for MPBicor requirement
--
-- 4) 	Add entry to the tdata_pool_ctl - Control table of MPBicor engine 
--
--
-- History 
-- Date         Developer          Revision          Remarks   
----------------------------------------------------------------------
-- 22/05/2004   Rickie Hui         Initial Version   MPBicor Migration
-- 15/10/2004   Rickie Hui	   MPBicor enhancement - Multi-cycle date
-- 09/12/2005   Rickie Hui	   Add Assignee copy - Assignee address handling
-- 06/06/2006   Rickie Hui         CoC macau address handling - use the second character '4' of policy no. = macau address
-- 22/05/2007   Rickie Hui         Enhancement for enlarge agency and agent full name to 45 and add new field agt_type
-- 06/09/2007   Rickie Hui         Enhancement to handle c.c. description not shown when Orphan Policy (agt_type = CSH or CSM)
-- 10/09/2007   Rickie Hui         Enhancement to extend the length of the field to handle long cc description
-- 12/24/2009   Rickie Hui         Enhancement for Bancassurance and direct marking project and corporate guildeline modification
-- 04/19/2010   Rickie Hui         Enhancement for Address Zone Enhancement 
-- 04/22/2010   Rickie Hui         Add with cheque count info in Cover Sheet for eAdvice project (MIS Request : AA-AA012138)
-- 07/07/2010   Rickie Hui         Add a Macau case field in the mpbicoríªs tables ( tdata_pool, tdata_hst ) and amend this sp to identity the  Macau case.
-- 11/14/2011   Rickie Hui         IT Request for Extend Client Name (to handle Long Name of Company Owner)( MIS request : AA-AA013661 )
-- 02/13/2014	Allen Liu	   H441 - add new csc_phone for CITI BANK
-- 04/28/2014   Walter Tan         HKG Integral Plus project 	
----------------------------------------------------------------------

    declare     @country char(4),
                @e_hdr_ftr_ver smallint,
                @c_hdr_ftr_ver smallint,
                @e_content_ver smallint,
                @c_content_ver smallint,
/* modified by Rickie Hui 11/14/2011 for extend client name (MIS request : AA-AA013661), changed the length from 43 to 50 */
/* Started */
		@insured varchar(50),
                @owner varchar(50),
                --@insured varchar(43),
                --@owner varchar(43),
/* Ended */
                @adr_1 varchar(35),
                @adr_2 varchar(35),
                @adr_3 varchar(35),
                @adr_4 varchar(35),
                @adr_5 varchar(35)

    declare     @prem_due_date datetime,
                @e_curr char(20),
                @c_curr char(20),
                @prem_amt decimal(15,2), 	  
	  /* modified by Rickie Hui 5/25/2007 for enlarge agy, agt full name to 45 and add new field agt_type */
	  /* Remarked >> start */
          /*      @serv_agency char(20),	*/
          /*      @serv_agent varchar(30),	*/
	  /* Remarked << End */
	  /* Added >> start */
                @serv_agency char(45),	
                @serv_agent varchar(45),	
	  /* Added << End */
                @serv_agny_phone char(10),
                @area_code char(2),
                @csc_phone varchar(30),
                @pay_mode char(2),
                @collect_office char(3),
                @ls_msg varchar(100),
                @cyc_date datetime,      
                @c_pay_mode_desc varchar(30),
                @e_pay_mode_desc varchar(30),
                @dept char(2),
                @desk char(3),
	  /* modified by Rickie Hui 5/25/2007 for extend the length of the field to handle long cc description */
	  /* Remarked >> start */    
          /*      @e_cc_to varchar(100),	*/
          /*      @c_cc_to varchar(100),	*/
	  /* Remarked << End */
	  /* Added >> start */   
                @e_cc_to varchar(150),                
	--	@e_cc_to_line1 varchar(90),
	--	@e_cc_to_line2 varchar(60),
		@c_cc_to varchar(150),
	--	@c_cc_to_line1 varchar(90),
	--	@c_cc_to_line2 varchar(60),
	  /* Added << end */   
                @copy_to varchar(100),
                @tel_no varchar(30)

	/* 12-09-2005 For Assignee Copy Handling */
	declare @add_assignee 	CHAR(1),
	        @assi_adr_1	CHAR(70),
	        @assi_adr_2 	CHAR(70),
	        @assi_adr_3 	CHAR(70),
	        @assi_adr_4	CHAR(70),
		@assi_adr_5	CHAR(70),
	  	@assignee	CHAR(86)
	/* 12-09-2005 End */

        /* modified by Rickie Hui 5/25/2007 for enlarge agy, agt full name to 45 and add new field agt_type */
  	/* Added >> start */    
	declare    @agt_type 		char(3),
		   @agt_type_desc_e 	char(50),
	   	   @agt_type_desc_c	char(50)
  	/* Added << End */

        /* added by Rickie Hui 04/19/2010 for Enhancement for Address Zone Enhancement */
  	/* Added >> start */   
	declare    @addrzone		varchar(5)
	/* Added << End */        

 /* Modified by Rickie Hui 07/07/2010 for Enhancement for Macau Case Enhancement*/ 
 /* Modified >> start */
        declare	   @macau_case 		char(10)
 /* Modified << end */

 /* added by Rickie Hui 6/10/2004 ==> */
 /*  declare     @li_letter_gen_seq   integer ,  */
     declare    @ls_letter_gen_seq varchar(13),		
	        @ls_cycle_date char(20)
                          
    select @cyc_date    = ( select current_cycle_date from db_print..tcycle_date where cycle_id = "LFCM" )
    if @@rowcount = 0 
    begin
        print "po_com_col_full: Cannot select current_cycle_date from db_print..tcycle_date which pol_no is "
        select @pol_no
        return -1
    end
 /* modified by Rickie Hui 6/6/2006 for CoC - macau address handling */
 /*   if substring(@pol_no,1,3) = 'B49' or substring(@pol_no,1,3) =' 49'   */

/* Enhancement for Bancassurance and direct marking project and corporate guildeline modification */
if exists(select 1 from t_mpb_new_template where doc_id = @doc_id)
begin
    if substring(@pol_no,2,1) = '4' 
    begin
        select @country = 'NMC'
 /* Modified by Rickie Hui 07/07/2010 for Enhancement for Macau Case Enhancement*/ 
 /* Modified >> start */
        select @macau_case = "MC"
 /* Modified << end */
    end
    else
    begin
        select @country = 'NHK'
 /* Modified by Rickie Hui 07/07/2010 for Enhancement for Macau Case Enhancement*/ 
 /* Modified >> start */
        select @macau_case = "HK"
 /* Modified << end */
    end
end
else
begin
 /* New added ===> */
    if substring(@pol_no,2,1) = '4' 
    begin
 /* New added <=== */
        select @country = 'MAC'
 /* Modified by Rickie Hui 07/07/2010 for Enhancement for Macau Case Enhancement*/ 
 /* Modified >> start */
        select @macau_case = "MC"
 /* Modified << end */
    end
    else
    begin
        select @country = 'HKG'
 /* Modified by Rickie Hui 07/07/2010 for Enhancement for Macau Case Enhancement*/ 
 /* Modified >> start */
	select @macau_case = "HK"
 /* Modified << end */
    end
end
    
    select @e_hdr_ftr_ver = ( select max(hdr_ftr_ver) from db_print..thdr_ftr
            where lang = 'ENG' and country = @country )
    select @c_hdr_ftr_ver = ( select max(hdr_ftr_ver) from db_print..thdr_ftr
            where lang = 'CHI' and country = @country )
    select @e_content_ver = ( select max(content_ver) from db_print..tcontent
            where lang = 'ENG' and doc_id = @doc_id )
    select @c_content_ver = ( select max(content_ver) from db_print..tcontent
            where lang = 'CHI' and doc_id = @doc_id )
                               
    set rowcount 1
    select @insured = ( select client_name from db_policy..tpol_name where insured = 'I'
            and comp_no = @comp_no and pol_no = @pol_no )
             

    select @owner =a.client_name
        from db_policy..tpol_name a
        where  a.owner = 'O' 
        and a.comp_no = @comp_no
        and a.pol_no = @pol_no

                                                                                                      

    /* Updated by Georgia Chau on 2 Jan 1998, Use payor's address instead of owner's, 
                          if owne's address not found

    select @owner =a.client_name, @adr_1 = b.adr_1, @adr_2=b.adr_2, 
        @adr_3=b.adr_3, @adr_4=b.adr_4, @adr_5=b.adr_5
        from db_policy..tpol_name a, db_policy..tpol_addr b
        where a.comp_no = b.comp_no
        and a.pol_no = b.pol_no
        and a.name_no = b.name_no
        and a.owner = 'O'' 
        and a.comp_no = @comp_no
        and a.pol_no = @pol_no
        and a.addr_cde = b.addr_cde
*/


    select  @adr_1 = b.adr_1, @adr_2=b.adr_2, 
        @adr_3=b.adr_3, @adr_4=b.adr_4, @adr_5=b.adr_5
        from db_policy..tpol_name a, db_policy..tpol_addr b
        where a.comp_no = b.comp_no
        and a.pol_no = b.pol_no
        and a.name_no = b.name_no
        and a.owner = 'O' 
        and a.comp_no = @comp_no
        and a.pol_no = @pol_no
        and a.addr_cde = b.addr_cde

    if @@rowcount=0
    begin
        select  @adr_1 = b.adr_1, @adr_2=b.adr_2, 
            @adr_3=b.adr_3, @adr_4=b.adr_4, @adr_5=b.adr_5
            from db_policy..tpol_name a, db_policy..tpol_addr b
            where a.comp_no = b.comp_no
                and a.pol_no = b.pol_no
                and a.name_no = b.name_no
                and a.payor = 'P' 
                and a.comp_no = @comp_no
                and a.pol_no = @pol_no
                and a.addr_cde = b.addr_cde
    end

    select @prem_due_date = ( select pay_to_dte from db_policy..tpol_mst where  
        comp_no = @comp_no and pol_no = @pol_no )       
    select @e_curr = ( select currdesc from db_policy..tpol_mst a, aidcconfig..currtab b 
        where a.pol_cur = b.currcode
        and a.comp_no = @comp_no
        and a.pol_no =  @pol_no )
    select @c_curr = ( select c_currdesc from db_policy..tpol_mst a, aidcconfig..currtab b 
        where a.pol_cur = b.currcode
        and a.comp_no = @comp_no
        and a.pol_no = @pol_no )
    select @prem_amt = ( select modal_prem_amt from db_policy..tpol_mst
        where comp_no = @comp_no
        and pol_no = @pol_no )
    if @prem_amt is null
       
select @prem_amt = 0.0
                            
    declare @r integer,
            @agy_code char(5),  
                                     
                                    
            @agy_adr1 char(35) ,
            @agy_adr2 char(35) ,
            @agy_adr3 char(35) ,
                                     
            @agy_l_code char(5) ,          
  /* modified by Rickie Hui 5/25/2007 for enlarge agy, agt full name to 45 and add new field agt_type */
  /* Remarked >> start */               
  /*        @agy_l_name char(30) ,                        */
  /* Remarked << end */
  /* Added >> start */
            @agy_l_name char(45) ,     
  /* Added << end */
            @agtcode char(5) 

    declare @optout char(1)     /* 02-26-2003 Agent Opt-Out */

  /* modified by Rickie Hui 5/25/2007 for enlarge agy, agt full name to 45 and add new field agt_type */
  /* Remarked >> start */                                   
  /*  exec @r = db_cor..po_ser_agt    '1', @comp_no, @pol_no, '0',	*/
  /* Remarked << end */
  /* Added >> start */
    exec @r = db_cor..po_ser_agt_full    '1', @comp_no, @pol_no, '0',
  /* Added << End */
            @agy_code  output,  
            @serv_agency output,                           
            @area_code output,        
            @agy_adr1  output,
            @agy_adr2  output,
            @agy_adr3  output,
            @serv_agny_phone output,                         
            @agy_l_code  output,                         
            @agy_l_name  output,                        
            @agtcode  output,
            @serv_agent output,
 /* modified by Rickie Hui 5/25/2007 for enlarge agy, agt full name to 45 and add new field agt_type */
 /* Added >> start */
            @agt_type output            
 /* Added << End */       

 /* modified by Rickie Hui 5/25/2007 for enlarge agy, agt full name to 45 and add new field agt_type */
 /* Added >> start				*/	
 exec aidcconfig..po_get_agttype_desc @agt_type, @agt_type_desc_e output, @agt_type_desc_c output
 /* Added << End 				*/                                                                                                                                                                                                                                                                                                              

 /* modified by Rickie Hui 9/6/2007 for handle c.c. description not shown when Orphan Policy */
 /* Remarked >> start */  
 /* if @agtcode = null or @agtcode <= '' */
 /* Remarked << End */
 /* modified by Rickie Hui 9/6/2007 for handle c.c. description not shown when Orphan Policy */
 /* Added >> start	*/
    if @agtcode = null or @agtcode <= '' or @agt_type = "CSH" or @agt_type = "CSM"
	/* New added for Bancassurance and Direct Marketing */
	or @agt_type = "BNK" or @agt_type = "DIR"
 /* Added << End 	*/
    begin
        select @e_cc_to = ''
        select @c_cc_to = ''
        select @copy_to = 'CUSTOMER!'       
    end
    else
    begin
   
        if @agy_code='00000' or @agy_code='09000' select @serv_agency='OTHER TERRITORIES (D8)'
        if @agy_code='05144' select @serv_agency='FSA COMPANY AGENT (28)'
        if @agy_code='88888' select @serv_agency='DEFAULT AGENT ONE (D8)'

/*      Updated by Georgia on 4 Feb 98  : 
    1.  If Tel. no. of agency phone no. is null or <= '', 
        suppress 'Tel :' in both e_cc_to / c_cc_to 
*/
        select @tel_no = rtrim(@serv_agny_phone)

        if @tel_no = null or @tel_no <= ''
        begin
            select @tel_no = ''
        end
        else
        begin
            select @tel_no = ' Tel.('+@tel_no+')'
        end
        select @e_cc_to = 'C.C. : ('+rtrim(@serv_agency)+')('+rtrim(@serv_agent)+')'+ @tel_no
        select @c_cc_to = '¦ãÑ+¡P : ('+rtrim(@serv_agency)+')('+rtrim(@serv_agent)+')'+ @tel_no   
	/* modified by Rickie Hui 5/25/2007 for extend the length of the field to handle long cc description */
	/* Added >> start */  
	--IF datalength(@e_cc_to) > 90
	--BEGIN
	--   select @e_cc_to_line1 = Left(@e_cc_to, 90)
	--   select @e_cc_to_line2 = Substring(@e_cc_to, 91, datalength(@e_cc_to)) 
	--   select @e_cc_to = @e_cc_to_line1 + ""
--""+@e_cc_to_line2
--	END
--
--	IF datalength(@c_cc_to) > 90
--	BEGIN
--	   select @c_cc_to_line1 = Left(@c_cc_to, 90)
--	   select @c_cc_to_line2 = Substring(@c_cc_to, 91, datalength(@c_cc_to)) 
--	   select @e_cc_to = @c_cc_to_line1 + ""
--""+@c_cc_to_line2
--	END
	/* Added << end */	
        select @copy_to = 'CUSTOMER!AGENT!'

        /* 02-26-2003 Agent Opt-Out */
        exec aidcconfig..po_optout_chk @agy_code, @agtcode, 'BICOR', @doc_id, @pol_no, @optout output
        if @optout = 'Y'
        begin
            select @copy_to = 'CUSTOMER!'
        end
        /* End 02-26-2003 */

	/* 12-09-2005 For Assignee Copy Handling */	
	
        exec po_chk_assignee @doc_id, @pol_no,  @assi_adr_1 output, @assi_adr_2 output, @assi_adr_3 output, @assi_adr_4 output, @assi_adr_5 output,@assignee output, @add_assignee output 
        if @add_assignee = 'Y'
        begin
            select @copy_to = @copy_to + 'ASSIGNEE!'
        end
	/* End 12-09-2005 */
    end 
                                
    if @country = 'MAC' or @country = "NMC"
        select @csc_phone = ( select mac_csc_phone from db_cor..tconfig )
    else
        select @csc_phone = ( select csc_phone from db_cor..tconfig )
    
    /* Enhancement for Bancassurance and direct marking project and corporate guildeline modification */
    if @agt_type = "BNK"
       if @country = "MAC" or @country = "NMC"
	  select @csc_phone = ( select remark from db_cor..tuw_misc where cat ="HOTL" and code = "BNKMAC")
       else
          select @csc_phone = ( select remark from db_cor..tuw_misc where cat ="HOTL" and code = "BNKHKG")
    /* 02/13/2014 begin */
    if @agt_type = "BNK" and exists (select 1 from aidcconfig..tplan_spec where comp_no='041' and action='CITI' and @pol_no like class_cde+sbase_cde+ssub_cde)
       begin
       if @country = "MAC" or @country = "NMC"
	  select @csc_phone = ( select remark from db_cor..tuw_misc where cat ="HOTL" and code = "CITIMAC")
       else
          select @csc_phone = ( select remark from db_cor..tuw_misc where cat ="HOTL" and code = "CITIHKG")
       end
    /* 02/13/2014 end */		
    if @agt_type = "DIR"
       if @country = "MAC" or @country = "NMC"
          select @csc_phone = ( select remark from db_cor..tuw_misc where cat ="HOTL" and code = "DIRMAC")
       else
          select @csc_phone = ( select remark from db_cor..tuw_misc where cat ="HOTL" and code = "DIRHKG")
   
    select @pay_mode = ( select pay_mode_cde from db_policy..tpol_mst
        where comp_no = @comp_no and pol_no = @pol_no ) 
                                                                                                             
                                                                
    select @c_pay_mode_desc = ( select c_pay_freq_desc
        from db_policy..tpay_freq
        where pay_freq_cde = @pay_mode )
    select @e_pay_mode_desc = ( select pay_freq_desc
        from db_policy..tpay_freq
        where pay_freq_cde = @pay_mode )
    select @collect_office = ( select coll_off from db_policy..tpol_international
        where comp_no = @comp_no and pol_no = @pol_no )

    /* added by Rickie Hui 04/19/2010 for Enhancement for Address Zone Enhancement */
    /* Added >> start */ 
    exec aidcconfig..po_get_address_zone @doc_id,1,@addrzone output
    /* Added << End */                                       
                             
    set rowcount 1
    /* Start Modify by Jinghua Xu,04/15/2015 */
    /*
    --Modify by Walter Tan for db_cor..tbsr_orig_req replacement for HKG Integral Plus project - 04/28/2014 >> Start
    /*select @dept = rout_dp, @desk = rout_desk from db_cor..tbsr_orig_req 
        where comp_no = @comp_no and pol_no = @pol_no*/
    select top 1 @dept=substring(usrprf,1,2), @desk=substring(usrprf,5,3)
    from db_policy..tpol_ptrntran
    where pol_no = @pol_no
    --Modify by Walter Tan for db_cor..tbsr_orig_req replacement for HKG Integral Plus project - 04/28/2014 << End
    */
    declare @user_profile varchar(20)
    select top 1 @user_profile = value from db_print..traw_data where cycle_date = @cyc_date and doc_id = @doc_id
    and pol_no = @pol_no and section_name = '###' and seq_no = '000' and IL_field_name = 'user_profile'

    exec db_print..po_doc_dpdsk @user_profile, @pol_no, @dept output, @desk output
    /* End Modify by Jinghua Xu,04/15/2015 */

    /* added by Rickie Hui 6/10/2004 ==> */    
    EXEC db_print..po_get_letrseq 1, @li_letter_gen_seq output
    select @ls_letter_gen_seq = CONVERT(varchar(13), @li_letter_gen_seq)
    select @ls_cycle_date = @cyc_date  

    /************insert into tdata_pool_ctl**********/                                      
    insert into db_print..tdata_pool_ctl                    
	( cycle_date, letter_gen_seq, user_key_1, user_key_2, user_key_3, user_key_4, user_key_5, letter_delete_ind)
		values                   
	( @cyc_date, @li_letter_gen_seq, @ls_cycle_date, @ls_letter_gen_seq, "041", @doc_id, @pol_no, 'N') 
    /* <== added by Rickie Hui 6/10/2004 */    
              
    insert into db_print..tdata_pool 
    (cycle_date, comp_no, doc_id,pol_no,country,
    c_hdr_ftr_ver,c_content_ver,
    e_hdr_ftr_ver,e_content_ver,
    insured,owner,adr_1,adr_2,adr_3,adr_4,adr_5,prem_due_date,e_curr,
    c_curr,prem_amt,serv_agency,serv_agent,serv_agny_phone,area_code,
    csc_phone,pay_mode,c_pay_mode_desc,e_pay_mode_desc,collect_office,
    dept,desk,skip_pgp,e_cc_to,c_cc_to,copy_to,
    date_1,date_2,date_3,date_4,date_5,
    dec_1,dec_2,dec_3,dec_4,dec_5,dec_6,dec_7,dec_8,dec_9,
    str_1,str_2,str_3,str_4,str_5,str_6,str_7,str_8,str_9, letter_gen_seq, 
    serv_agency_code, serv_agent_code,
 /* modified by Rickie Hui 5/25/2007 for enlarge agy, agt full name to 45 and add new field agt_type */
 /* Remarked >> start */
 /*   assi_adr_1, assi_adr_2, assi_adr_3, assi_adr_4, assi_adr_5, assignee)	*/
 /* Remarked << end */
 /* Added >> start */
    assi_adr_1, assi_adr_2, assi_adr_3, assi_adr_4, assi_adr_5, assignee, agt_type, agt_type_desc_e, agt_type_desc_c, 
 /* Added << end */
 /* added by Rickie Hui 04/19/2010 for Enhancement for Address Zone Enhancement and withcheque Enhancement*/ 
 /* Added >> start */
 /* Modified by Rickie Hui 07/07/2010 for Enhancement for Macau Case Enhancement*/ 
 /* Modified >> start */
    addrzone, withcheque, macau_case)
 /* Modified << end */
 /* Added << end */
    values( @cyc_date,  @comp_no, @doc_id,  @pol_no, @country, 
        @c_hdr_ftr_ver, @c_content_ver,
        @e_hdr_ftr_ver, @e_content_ver,
        @insured, @owner, @adr_1, @adr_2, @adr_3, @adr_4, @adr_5, @prem_due_date, @e_curr,
        @c_curr, @prem_amt, @serv_agency, @serv_agent, @serv_agny_phone, @area_code,
        @csc_phone, @pay_mode, @c_pay_mode_desc,@e_pay_mode_desc, @collect_office,
        @dept,@desk,null,@e_cc_to,@c_cc_to,@copy_to,
        null,null,null,null,null,
        null,null,null,null,null,null,null,null,null,
        null,null,null,null,null,null,null,null,null,@li_letter_gen_seq,
	@agy_code, @agtcode,
 /* modified by Rickie Hui 5/25/2007 for enlarge agy, agt full name to 45 and add new field agt_type */
 /* Remarked >> start */
 /*	@assi_adr_1, @assi_adr_2, @assi_adr_3, @assi_adr_4, @assi_adr_5, @assignee)	*/
 /* Remarked << end */
 /* Added >> start */
	@assi_adr_1, @assi_adr_2, @assi_adr_3, @assi_adr_4, @assi_adr_5, @assignee, @agt_type, @agt_type_desc_e, @agt_type_desc_c, 
 /* Added << end */
 /* added by Rickie Hui 04/19/2010 for Enhancement for Address Zone Enhancement and withcheque Enhancement*/
 /* Added >> start */
 /* Modified by Rickie Hui 07/07/2010 for Enhancement for Macau Case Enhancement*/ 
 /* Modified >> start */
    @addrzone, null, @macau_case)
 /* Modified << end */
 /* Added << end */
    if @@error != 0
     
    goto error_exit   

    IF @@trancount != 0
        COMMIT TRANSACTION
    set rowcount 0
    return

    error_exit:
   IF @@trancount != 0
  ROLLBACK TRANSACTION
   SELECT @ls_msg = "po_com_col_full: cannot insert db_print..tdata_pool." 
   SELECT @ls_msg
   return -1