package org.oxymores.chronix.dto;

public class DTORRule
{
	public String name;
	public String description;
	public String id;

	public String period;
	public int interval;

	// ByDay (01 = Monday, 07 = Sunday)
	public boolean bd_01 = false, bd_02 = false, bd_03 = false, bd_04 = false, bd_05 = false, bd_06 = false, bd_07 = false;
	// ByMonthDay
	public boolean bmd_01 = false, bmd_02 = false, bmd_03 = false, bmd_04 = false, bmd_05 = false, bmd_06 = false, bmd_07 = false,
			bmd_08 = false, bmd_09 = false, bmd_10 = false, bmd_11 = false, bmd_12 = false, bmd_13 = false, bmd_14 = false, bmd_15 = false,
			bmd_16 = false, bmd_17 = false, bmd_18 = false, bmd_19 = false, bmd_20 = false, bmd_21 = false, bmd_22 = false, bmd_23 = false,
			bmd_24 = false, bmd_25 = false, bmd_26 = false, bmd_27 = false, bmd_28 = false, bmd_29 = false, bmd_30 = false, bmd_31 = false;
	public boolean bmdn_01 = false, bmdn_02 = false, bmdn_03 = false, bmdn_04 = false, bmdn_05 = false, bmdn_06 = false, bmdn_07 = false,
			bmdn_08 = false, bmdn_09 = false, bmdn_10 = false, bmdn_11 = false, bmdn_12 = false, bmdn_13 = false, bmdn_14 = false,
			bmdn_15 = false, bmdn_16 = false, bmdn_17 = false, bmdn_18 = false, bmdn_19 = false, bmdn_20 = false, bmdn_21 = false,
			bmdn_22 = false, bmdn_23 = false, bmdn_24 = false, bmdn_25 = false, bmdn_26 = false, bmdn_27 = false, bmdn_28 = false,
			bmdn_29 = false, bmdn_30 = false, bmdn_31 = false;
	// ByMonth (01 = January)
	public boolean bm_01 = false, bm_02 = false, bm_03 = false, bm_04 = false, bm_05 = false, bm_06 = false, bm_07 = false, bm_08 = false,
			bm_09 = false, bm_10 = false, bm_11 = false, bm_12 = false;
	// ByHour
	public boolean bh_00 = false, bh_01 = false, bh_02 = false, bh_03 = false, bh_04 = false, bh_05 = false, bh_06 = false, bh_07 = false,
			bh_08 = false, bh_09 = false, bh_10 = false, bh_11 = false, bh_12 = false, bh_13 = false, bh_14 = false, bh_15 = false,
			bh_16 = false, bh_17 = false, bh_18 = false, bh_19 = false, bh_20 = false, bh_21 = false, bh_22 = false, bh_23 = false;
	// ByMinute
	public boolean bn_00 = false, bn_01 = false, bn_02 = false, bn_03 = false, bn_04 = false, bn_05 = false, bn_06 = false, bn_07 = false,
			bn_08 = false, bn_09 = false, bn_10 = false, bn_11 = false, bn_12 = false, bn_13 = false, bn_14 = false, bn_15 = false,
			bn_16 = false, bn_17 = false, bn_18 = false, bn_19 = false, bn_20 = false, bn_21 = false, bn_22 = false, bn_23 = false,
			bn_24 = false, bn_25 = false, bn_26 = false, bn_27 = false, bn_28 = false, bn_29 = false, bn_30 = false, bn_31 = false,
			bn_32 = false, bn_33 = false, bn_34 = false, bn_35 = false, bn_36 = false, bn_37 = false, bn_38 = false, bn_39 = false,
			bn_40 = false, bn_41 = false, bn_42 = false, bn_43 = false, bn_44 = false, bn_45 = false, bn_46 = false, bn_47 = false,
			bn_48 = false, bn_49 = false, bn_50 = false, bn_51 = false, bn_52 = false, bn_53 = false, bn_54 = false, bn_55 = false,
			bn_56 = false, bn_57 = false, bn_58 = false, bn_59 = false;

	// Stupid get/set for Javabean convention
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getPeriod()
	{
		return period;
	}

	public void setPeriod(String period)
	{
		this.period = period;
	}

	public int getInterval()
	{
		return interval;
	}

	public void setInterval(int interval)
	{
		this.interval = interval;
	}

	public boolean isBmd_01()
	{
		return bmd_01;
	}

	public void setBmd_01(boolean bmd_01)
	{
		this.bmd_01 = bmd_01;
	}

	public boolean isBmd_02()
	{
		return bmd_02;
	}

	public void setBmd_02(boolean bmd_02)
	{
		this.bmd_02 = bmd_02;
	}

	public boolean isBmd_03()
	{
		return bmd_03;
	}

	public void setBmd_03(boolean bmd_03)
	{
		this.bmd_03 = bmd_03;
	}

	public boolean isBmd_04()
	{
		return bmd_04;
	}

	public void setBmd_04(boolean bmd_04)
	{
		this.bmd_04 = bmd_04;
	}

	public boolean isBmd_05()
	{
		return bmd_05;
	}

	public void setBmd_05(boolean bmd_05)
	{
		this.bmd_05 = bmd_05;
	}

	public boolean isBmd_06()
	{
		return bmd_06;
	}

	public void setBmd_06(boolean bmd_06)
	{
		this.bmd_06 = bmd_06;
	}

	public boolean isBmd_07()
	{
		return bmd_07;
	}

	public void setBmd_07(boolean bmd_07)
	{
		this.bmd_07 = bmd_07;
	}

	public boolean isBmd_08()
	{
		return bmd_08;
	}

	public void setBmd_08(boolean bmd_08)
	{
		this.bmd_08 = bmd_08;
	}

	public boolean isBmd_09()
	{
		return bmd_09;
	}

	public void setBmd_09(boolean bmd_09)
	{
		this.bmd_09 = bmd_09;
	}

	public boolean isBmd_10()
	{
		return bmd_10;
	}

	public void setBmd_10(boolean bmd_10)
	{
		this.bmd_10 = bmd_10;
	}

	public boolean isBmd_11()
	{
		return bmd_11;
	}

	public void setBmd_11(boolean bmd_11)
	{
		this.bmd_11 = bmd_11;
	}

	public boolean isBmd_12()
	{
		return bmd_12;
	}

	public void setBmd_12(boolean bmd_12)
	{
		this.bmd_12 = bmd_12;
	}

	public boolean isBmd_13()
	{
		return bmd_13;
	}

	public void setBmd_13(boolean bmd_13)
	{
		this.bmd_13 = bmd_13;
	}

	public boolean isBmd_14()
	{
		return bmd_14;
	}

	public void setBmd_14(boolean bmd_14)
	{
		this.bmd_14 = bmd_14;
	}

	public boolean isBmd_15()
	{
		return bmd_15;
	}

	public void setBmd_15(boolean bmd_15)
	{
		this.bmd_15 = bmd_15;
	}

	public boolean isBmd_16()
	{
		return bmd_16;
	}

	public void setBmd_16(boolean bmd_16)
	{
		this.bmd_16 = bmd_16;
	}

	public boolean isBmd_17()
	{
		return bmd_17;
	}

	public void setBmd_17(boolean bmd_17)
	{
		this.bmd_17 = bmd_17;
	}

	public boolean isBmd_18()
	{
		return bmd_18;
	}

	public void setBmd_18(boolean bmd_18)
	{
		this.bmd_18 = bmd_18;
	}

	public boolean isBmd_19()
	{
		return bmd_19;
	}

	public void setBmd_19(boolean bmd_19)
	{
		this.bmd_19 = bmd_19;
	}

	public boolean isBmd_20()
	{
		return bmd_20;
	}

	public void setBmd_20(boolean bmd_20)
	{
		this.bmd_20 = bmd_20;
	}

	public boolean isBmd_21()
	{
		return bmd_21;
	}

	public void setBmd_21(boolean bmd_21)
	{
		this.bmd_21 = bmd_21;
	}

	public boolean isBmd_22()
	{
		return bmd_22;
	}

	public void setBmd_22(boolean bmd_22)
	{
		this.bmd_22 = bmd_22;
	}

	public boolean isBmd_23()
	{
		return bmd_23;
	}

	public void setBmd_23(boolean bmd_23)
	{
		this.bmd_23 = bmd_23;
	}

	public boolean isBmd_24()
	{
		return bmd_24;
	}

	public void setBmd_24(boolean bmd_24)
	{
		this.bmd_24 = bmd_24;
	}

	public boolean isBmd_25()
	{
		return bmd_25;
	}

	public void setBmd_25(boolean bmd_25)
	{
		this.bmd_25 = bmd_25;
	}

	public boolean isBmd_26()
	{
		return bmd_26;
	}

	public void setBmd_26(boolean bmd_26)
	{
		this.bmd_26 = bmd_26;
	}

	public boolean isBmd_27()
	{
		return bmd_27;
	}

	public void setBmd_27(boolean bmd_27)
	{
		this.bmd_27 = bmd_27;
	}

	public boolean isBmd_28()
	{
		return bmd_28;
	}

	public void setBmd_28(boolean bmd_28)
	{
		this.bmd_28 = bmd_28;
	}

	public boolean isBmd_29()
	{
		return bmd_29;
	}

	public void setBmd_29(boolean bmd_29)
	{
		this.bmd_29 = bmd_29;
	}

	public boolean isBmd_30()
	{
		return bmd_30;
	}

	public void setBmd_30(boolean bmd_30)
	{
		this.bmd_30 = bmd_30;
	}

	public boolean isBmd_31()
	{
		return bmd_31;
	}

	public void setBmd_31(boolean bmd_31)
	{
		this.bmd_31 = bmd_31;
	}

	public boolean isBmdn_01()
	{
		return bmdn_01;
	}

	public void setBmdn_01(boolean bmdn_01)
	{
		this.bmdn_01 = bmdn_01;
	}

	public boolean isBmdn_02()
	{
		return bmdn_02;
	}

	public void setBmdn_02(boolean bmdn_02)
	{
		this.bmdn_02 = bmdn_02;
	}

	public boolean isBmdn_03()
	{
		return bmdn_03;
	}

	public void setBmdn_03(boolean bmdn_03)
	{
		this.bmdn_03 = bmdn_03;
	}

	public boolean isBmdn_04()
	{
		return bmdn_04;
	}

	public void setBmdn_04(boolean bmdn_04)
	{
		this.bmdn_04 = bmdn_04;
	}

	public boolean isBmdn_05()
	{
		return bmdn_05;
	}

	public void setBmdn_05(boolean bmdn_05)
	{
		this.bmdn_05 = bmdn_05;
	}

	public boolean isBmdn_06()
	{
		return bmdn_06;
	}

	public void setBmdn_06(boolean bmdn_06)
	{
		this.bmdn_06 = bmdn_06;
	}

	public boolean isBmdn_07()
	{
		return bmdn_07;
	}

	public void setBmdn_07(boolean bmdn_07)
	{
		this.bmdn_07 = bmdn_07;
	}

	public boolean isBmdn_08()
	{
		return bmdn_08;
	}

	public void setBmdn_08(boolean bmdn_08)
	{
		this.bmdn_08 = bmdn_08;
	}

	public boolean isBmdn_09()
	{
		return bmdn_09;
	}

	public void setBmdn_09(boolean bmdn_09)
	{
		this.bmdn_09 = bmdn_09;
	}

	public boolean isBmdn_10()
	{
		return bmdn_10;
	}

	public void setBmdn_10(boolean bmdn_10)
	{
		this.bmdn_10 = bmdn_10;
	}

	public boolean isBmdn_11()
	{
		return bmdn_11;
	}

	public void setBmdn_11(boolean bmdn_11)
	{
		this.bmdn_11 = bmdn_11;
	}

	public boolean isBmdn_12()
	{
		return bmdn_12;
	}

	public void setBmdn_12(boolean bmdn_12)
	{
		this.bmdn_12 = bmdn_12;
	}

	public boolean isBmdn_13()
	{
		return bmdn_13;
	}

	public void setBmdn_13(boolean bmdn_13)
	{
		this.bmdn_13 = bmdn_13;
	}

	public boolean isBmdn_14()
	{
		return bmdn_14;
	}

	public void setBmdn_14(boolean bmdn_14)
	{
		this.bmdn_14 = bmdn_14;
	}

	public boolean isBmdn_15()
	{
		return bmdn_15;
	}

	public void setBmdn_15(boolean bmdn_15)
	{
		this.bmdn_15 = bmdn_15;
	}

	public boolean isBmdn_16()
	{
		return bmdn_16;
	}

	public void setBmdn_16(boolean bmdn_16)
	{
		this.bmdn_16 = bmdn_16;
	}

	public boolean isBmdn_17()
	{
		return bmdn_17;
	}

	public void setBmdn_17(boolean bmdn_17)
	{
		this.bmdn_17 = bmdn_17;
	}

	public boolean isBmdn_18()
	{
		return bmdn_18;
	}

	public void setBmdn_18(boolean bmdn_18)
	{
		this.bmdn_18 = bmdn_18;
	}

	public boolean isBmdn_19()
	{
		return bmdn_19;
	}

	public void setBmdn_19(boolean bmdn_19)
	{
		this.bmdn_19 = bmdn_19;
	}

	public boolean isBmdn_20()
	{
		return bmdn_20;
	}

	public void setBmdn_20(boolean bmdn_20)
	{
		this.bmdn_20 = bmdn_20;
	}

	public boolean isBmdn_21()
	{
		return bmdn_21;
	}

	public void setBmdn_21(boolean bmdn_21)
	{
		this.bmdn_21 = bmdn_21;
	}

	public boolean isBmdn_22()
	{
		return bmdn_22;
	}

	public void setBmdn_22(boolean bmdn_22)
	{
		this.bmdn_22 = bmdn_22;
	}

	public boolean isBmdn_23()
	{
		return bmdn_23;
	}

	public void setBmdn_23(boolean bmdn_23)
	{
		this.bmdn_23 = bmdn_23;
	}

	public boolean isBmdn_24()
	{
		return bmdn_24;
	}

	public void setBmdn_24(boolean bmdn_24)
	{
		this.bmdn_24 = bmdn_24;
	}

	public boolean isBmdn_25()
	{
		return bmdn_25;
	}

	public void setBmdn_25(boolean bmdn_25)
	{
		this.bmdn_25 = bmdn_25;
	}

	public boolean isBmdn_26()
	{
		return bmdn_26;
	}

	public void setBmdn_26(boolean bmdn_26)
	{
		this.bmdn_26 = bmdn_26;
	}

	public boolean isBmdn_27()
	{
		return bmdn_27;
	}

	public void setBmdn_27(boolean bmdn_27)
	{
		this.bmdn_27 = bmdn_27;
	}

	public boolean isBmdn_28()
	{
		return bmdn_28;
	}

	public void setBmdn_28(boolean bmdn_28)
	{
		this.bmdn_28 = bmdn_28;
	}

	public boolean isBmdn_29()
	{
		return bmdn_29;
	}

	public void setBmdn_29(boolean bmdn_29)
	{
		this.bmdn_29 = bmdn_29;
	}

	public boolean isBmdn_30()
	{
		return bmdn_30;
	}

	public void setBmdn_30(boolean bmdn_30)
	{
		this.bmdn_30 = bmdn_30;
	}

	public boolean isBmdn_31()
	{
		return bmdn_31;
	}

	public void setBmdn_31(boolean bmdn_31)
	{
		this.bmdn_31 = bmdn_31;
	}

	public boolean isBh_00()
	{
		return bh_00;
	}

	public void setBh_00(boolean bh_00)
	{
		this.bh_00 = bh_00;
	}

	public boolean isBh_01()
	{
		return bh_01;
	}

	public void setBh_01(boolean bh_01)
	{
		this.bh_01 = bh_01;
	}

	public boolean isBh_02()
	{
		return bh_02;
	}

	public void setBh_02(boolean bh_02)
	{
		this.bh_02 = bh_02;
	}

	public boolean isBh_03()
	{
		return bh_03;
	}

	public void setBh_03(boolean bh_03)
	{
		this.bh_03 = bh_03;
	}

	public boolean isBh_04()
	{
		return bh_04;
	}

	public void setBh_04(boolean bh_04)
	{
		this.bh_04 = bh_04;
	}

	public boolean isBh_05()
	{
		return bh_05;
	}

	public void setBh_05(boolean bh_05)
	{
		this.bh_05 = bh_05;
	}

	public boolean isBh_06()
	{
		return bh_06;
	}

	public void setBh_06(boolean bh_06)
	{
		this.bh_06 = bh_06;
	}

	public boolean isBh_07()
	{
		return bh_07;
	}

	public void setBh_07(boolean bh_07)
	{
		this.bh_07 = bh_07;
	}

	public boolean isBh_08()
	{
		return bh_08;
	}

	public void setBh_08(boolean bh_08)
	{
		this.bh_08 = bh_08;
	}

	public boolean isBh_09()
	{
		return bh_09;
	}

	public void setBh_09(boolean bh_09)
	{
		this.bh_09 = bh_09;
	}

	public boolean isBh_10()
	{
		return bh_10;
	}

	public void setBh_10(boolean bh_10)
	{
		this.bh_10 = bh_10;
	}

	public boolean isBh_11()
	{
		return bh_11;
	}

	public void setBh_11(boolean bh_11)
	{
		this.bh_11 = bh_11;
	}

	public boolean isBh_12()
	{
		return bh_12;
	}

	public void setBh_12(boolean bh_12)
	{
		this.bh_12 = bh_12;
	}

	public boolean isBh_13()
	{
		return bh_13;
	}

	public void setBh_13(boolean bh_13)
	{
		this.bh_13 = bh_13;
	}

	public boolean isBh_14()
	{
		return bh_14;
	}

	public void setBh_14(boolean bh_14)
	{
		this.bh_14 = bh_14;
	}

	public boolean isBh_15()
	{
		return bh_15;
	}

	public void setBh_15(boolean bh_15)
	{
		this.bh_15 = bh_15;
	}

	public boolean isBh_16()
	{
		return bh_16;
	}

	public void setBh_16(boolean bh_16)
	{
		this.bh_16 = bh_16;
	}

	public boolean isBh_17()
	{
		return bh_17;
	}

	public void setBh_17(boolean bh_17)
	{
		this.bh_17 = bh_17;
	}

	public boolean isBh_18()
	{
		return bh_18;
	}

	public void setBh_18(boolean bh_18)
	{
		this.bh_18 = bh_18;
	}

	public boolean isBh_19()
	{
		return bh_19;
	}

	public void setBh_19(boolean bh_19)
	{
		this.bh_19 = bh_19;
	}

	public boolean isBh_20()
	{
		return bh_20;
	}

	public void setBh_20(boolean bh_20)
	{
		this.bh_20 = bh_20;
	}

	public boolean isBh_21()
	{
		return bh_21;
	}

	public void setBh_21(boolean bh_21)
	{
		this.bh_21 = bh_21;
	}

	public boolean isBh_22()
	{
		return bh_22;
	}

	public void setBh_22(boolean bh_22)
	{
		this.bh_22 = bh_22;
	}

	public boolean isBh_23()
	{
		return bh_23;
	}

	public void setBh_23(boolean bh_23)
	{
		this.bh_23 = bh_23;
	}

	public boolean isBd_01()
	{
		return bd_01;
	}

	public void setBd_01(boolean bd_01)
	{
		this.bd_01 = bd_01;
	}

	public boolean isBd_02()
	{
		return bd_02;
	}

	public void setBd_02(boolean bd_02)
	{
		this.bd_02 = bd_02;
	}

	public boolean isBd_03()
	{
		return bd_03;
	}

	public void setBd_03(boolean bd_03)
	{
		this.bd_03 = bd_03;
	}

	public boolean isBd_04()
	{
		return bd_04;
	}

	public void setBd_04(boolean bd_04)
	{
		this.bd_04 = bd_04;
	}

	public boolean isBd_05()
	{
		return bd_05;
	}

	public void setBd_05(boolean bd_05)
	{
		this.bd_05 = bd_05;
	}

	public boolean isBd_06()
	{
		return bd_06;
	}

	public void setBd_06(boolean bd_06)
	{
		this.bd_06 = bd_06;
	}

	public boolean isBd_07()
	{
		return bd_07;
	}

	public void setBd_07(boolean bd_07)
	{
		this.bd_07 = bd_07;
	}

	public boolean isBm_01()
	{
		return bm_01;
	}

	public void setBm_01(boolean bm_01)
	{
		this.bm_01 = bm_01;
	}

	public boolean isBm_02()
	{
		return bm_02;
	}

	public void setBm_02(boolean bm_02)
	{
		this.bm_02 = bm_02;
	}

	public boolean isBm_03()
	{
		return bm_03;
	}

	public void setBm_03(boolean bm_03)
	{
		this.bm_03 = bm_03;
	}

	public boolean isBm_04()
	{
		return bm_04;
	}

	public void setBm_04(boolean bm_04)
	{
		this.bm_04 = bm_04;
	}

	public boolean isBm_05()
	{
		return bm_05;
	}

	public void setBm_05(boolean bm_05)
	{
		this.bm_05 = bm_05;
	}

	public boolean isBm_06()
	{
		return bm_06;
	}

	public void setBm_06(boolean bm_06)
	{
		this.bm_06 = bm_06;
	}

	public boolean isBm_07()
	{
		return bm_07;
	}

	public void setBm_07(boolean bm_07)
	{
		this.bm_07 = bm_07;
	}

	public boolean isBm_08()
	{
		return bm_08;
	}

	public void setBm_08(boolean bm_08)
	{
		this.bm_08 = bm_08;
	}

	public boolean isBm_09()
	{
		return bm_09;
	}

	public void setBm_09(boolean bm_09)
	{
		this.bm_09 = bm_09;
	}

	public boolean isBm_10()
	{
		return bm_10;
	}

	public void setBm_10(boolean bm_10)
	{
		this.bm_10 = bm_10;
	}

	public boolean isBm_11()
	{
		return bm_11;
	}

	public void setBm_11(boolean bm_11)
	{
		this.bm_11 = bm_11;
	}

	public boolean isBm_12()
	{
		return bm_12;
	}

	public void setBm_12(boolean bm_12)
	{
		this.bm_12 = bm_12;
	}

	public boolean isBn_00()
	{
		return bn_00;
	}

	public void setBn_00(boolean bn_00)
	{
		this.bn_00 = bn_00;
	}

	public boolean isBn_01()
	{
		return bn_01;
	}

	public void setBn_01(boolean bn_01)
	{
		this.bn_01 = bn_01;
	}

	public boolean isBn_02()
	{
		return bn_02;
	}

	public void setBn_02(boolean bn_02)
	{
		this.bn_02 = bn_02;
	}

	public boolean isBn_03()
	{
		return bn_03;
	}

	public void setBn_03(boolean bn_03)
	{
		this.bn_03 = bn_03;
	}

	public boolean isBn_04()
	{
		return bn_04;
	}

	public void setBn_04(boolean bn_04)
	{
		this.bn_04 = bn_04;
	}

	public boolean isBn_05()
	{
		return bn_05;
	}

	public void setBn_05(boolean bn_05)
	{
		this.bn_05 = bn_05;
	}

	public boolean isBn_06()
	{
		return bn_06;
	}

	public void setBn_06(boolean bn_06)
	{
		this.bn_06 = bn_06;
	}

	public boolean isBn_07()
	{
		return bn_07;
	}

	public void setBn_07(boolean bn_07)
	{
		this.bn_07 = bn_07;
	}

	public boolean isBn_08()
	{
		return bn_08;
	}

	public void setBn_08(boolean bn_08)
	{
		this.bn_08 = bn_08;
	}

	public boolean isBn_09()
	{
		return bn_09;
	}

	public void setBn_09(boolean bn_09)
	{
		this.bn_09 = bn_09;
	}

	public boolean isBn_10()
	{
		return bn_10;
	}

	public void setBn_10(boolean bn_10)
	{
		this.bn_10 = bn_10;
	}

	public boolean isBn_11()
	{
		return bn_11;
	}

	public void setBn_11(boolean bn_11)
	{
		this.bn_11 = bn_11;
	}

	public boolean isBn_12()
	{
		return bn_12;
	}

	public void setBn_12(boolean bn_12)
	{
		this.bn_12 = bn_12;
	}

	public boolean isBn_13()
	{
		return bn_13;
	}

	public void setBn_13(boolean bn_13)
	{
		this.bn_13 = bn_13;
	}

	public boolean isBn_14()
	{
		return bn_14;
	}

	public void setBn_14(boolean bn_14)
	{
		this.bn_14 = bn_14;
	}

	public boolean isBn_15()
	{
		return bn_15;
	}

	public void setBn_15(boolean bn_15)
	{
		this.bn_15 = bn_15;
	}

	public boolean isBn_16()
	{
		return bn_16;
	}

	public void setBn_16(boolean bn_16)
	{
		this.bn_16 = bn_16;
	}

	public boolean isBn_17()
	{
		return bn_17;
	}

	public void setBn_17(boolean bn_17)
	{
		this.bn_17 = bn_17;
	}

	public boolean isBn_18()
	{
		return bn_18;
	}

	public void setBn_18(boolean bn_18)
	{
		this.bn_18 = bn_18;
	}

	public boolean isBn_19()
	{
		return bn_19;
	}

	public void setBn_19(boolean bn_19)
	{
		this.bn_19 = bn_19;
	}

	public boolean isBn_20()
	{
		return bn_20;
	}

	public void setBn_20(boolean bn_20)
	{
		this.bn_20 = bn_20;
	}

	public boolean isBn_21()
	{
		return bn_21;
	}

	public void setBn_21(boolean bn_21)
	{
		this.bn_21 = bn_21;
	}

	public boolean isBn_22()
	{
		return bn_22;
	}

	public void setBn_22(boolean bn_22)
	{
		this.bn_22 = bn_22;
	}

	public boolean isBn_23()
	{
		return bn_23;
	}

	public void setBn_23(boolean bn_23)
	{
		this.bn_23 = bn_23;
	}

	public boolean isBn_24()
	{
		return bn_24;
	}

	public void setBn_24(boolean bn_24)
	{
		this.bn_24 = bn_24;
	}

	public boolean isBn_25()
	{
		return bn_25;
	}

	public void setBn_25(boolean bn_25)
	{
		this.bn_25 = bn_25;
	}

	public boolean isBn_26()
	{
		return bn_26;
	}

	public void setBn_26(boolean bn_26)
	{
		this.bn_26 = bn_26;
	}

	public boolean isBn_27()
	{
		return bn_27;
	}

	public void setBn_27(boolean bn_27)
	{
		this.bn_27 = bn_27;
	}

	public boolean isBn_28()
	{
		return bn_28;
	}

	public void setBn_28(boolean bn_28)
	{
		this.bn_28 = bn_28;
	}

	public boolean isBn_29()
	{
		return bn_29;
	}

	public void setBn_29(boolean bn_29)
	{
		this.bn_29 = bn_29;
	}

	public boolean isBn_30()
	{
		return bn_30;
	}

	public void setBn_30(boolean bn_30)
	{
		this.bn_30 = bn_30;
	}

	public boolean isBn_31()
	{
		return bn_31;
	}

	public void setBn_31(boolean bn_31)
	{
		this.bn_31 = bn_31;
	}

	public boolean isBn_32()
	{
		return bn_32;
	}

	public void setBn_32(boolean bn_32)
	{
		this.bn_32 = bn_32;
	}

	public boolean isBn_33()
	{
		return bn_33;
	}

	public void setBn_33(boolean bn_33)
	{
		this.bn_33 = bn_33;
	}

	public boolean isBn_34()
	{
		return bn_34;
	}

	public void setBn_34(boolean bn_34)
	{
		this.bn_34 = bn_34;
	}

	public boolean isBn_35()
	{
		return bn_35;
	}

	public void setBn_35(boolean bn_35)
	{
		this.bn_35 = bn_35;
	}

	public boolean isBn_36()
	{
		return bn_36;
	}

	public void setBn_36(boolean bn_36)
	{
		this.bn_36 = bn_36;
	}

	public boolean isBn_37()
	{
		return bn_37;
	}

	public void setBn_37(boolean bn_37)
	{
		this.bn_37 = bn_37;
	}

	public boolean isBn_38()
	{
		return bn_38;
	}

	public void setBn_38(boolean bn_38)
	{
		this.bn_38 = bn_38;
	}

	public boolean isBn_39()
	{
		return bn_39;
	}

	public void setBn_39(boolean bn_39)
	{
		this.bn_39 = bn_39;
	}

	public boolean isBn_40()
	{
		return bn_40;
	}

	public void setBn_40(boolean bn_40)
	{
		this.bn_40 = bn_40;
	}

	public boolean isBn_41()
	{
		return bn_41;
	}

	public void setBn_41(boolean bn_41)
	{
		this.bn_41 = bn_41;
	}

	public boolean isBn_42()
	{
		return bn_42;
	}

	public void setBn_42(boolean bn_42)
	{
		this.bn_42 = bn_42;
	}

	public boolean isBn_43()
	{
		return bn_43;
	}

	public void setBn_43(boolean bn_43)
	{
		this.bn_43 = bn_43;
	}

	public boolean isBn_44()
	{
		return bn_44;
	}

	public void setBn_44(boolean bn_44)
	{
		this.bn_44 = bn_44;
	}

	public boolean isBn_45()
	{
		return bn_45;
	}

	public void setBn_45(boolean bn_45)
	{
		this.bn_45 = bn_45;
	}

	public boolean isBn_46()
	{
		return bn_46;
	}

	public void setBn_46(boolean bn_46)
	{
		this.bn_46 = bn_46;
	}

	public boolean isBn_47()
	{
		return bn_47;
	}

	public void setBn_47(boolean bn_47)
	{
		this.bn_47 = bn_47;
	}

	public boolean isBn_48()
	{
		return bn_48;
	}

	public void setBn_48(boolean bn_48)
	{
		this.bn_48 = bn_48;
	}

	public boolean isBn_49()
	{
		return bn_49;
	}

	public void setBn_49(boolean bn_49)
	{
		this.bn_49 = bn_49;
	}

	public boolean isBn_50()
	{
		return bn_50;
	}

	public void setBn_50(boolean bn_50)
	{
		this.bn_50 = bn_50;
	}

	public boolean isBn_51()
	{
		return bn_51;
	}

	public void setBn_51(boolean bn_51)
	{
		this.bn_51 = bn_51;
	}

	public boolean isBn_52()
	{
		return bn_52;
	}

	public void setBn_52(boolean bn_52)
	{
		this.bn_52 = bn_52;
	}

	public boolean isBn_53()
	{
		return bn_53;
	}

	public void setBn_53(boolean bn_53)
	{
		this.bn_53 = bn_53;
	}

	public boolean isBn_54()
	{
		return bn_54;
	}

	public void setBn_54(boolean bn_54)
	{
		this.bn_54 = bn_54;
	}

	public boolean isBn_55()
	{
		return bn_55;
	}

	public void setBn_55(boolean bn_55)
	{
		this.bn_55 = bn_55;
	}

	public boolean isBn_56()
	{
		return bn_56;
	}

	public void setBn_56(boolean bn_56)
	{
		this.bn_56 = bn_56;
	}

	public boolean isBn_57()
	{
		return bn_57;
	}

	public void setBn_57(boolean bn_57)
	{
		this.bn_57 = bn_57;
	}

	public boolean isBn_58()
	{
		return bn_58;
	}

	public void setBn_58(boolean bn_58)
	{
		this.bn_58 = bn_58;
	}

	public boolean isBn_59()
	{
		return bn_59;
	}

	public void setBn_59(boolean bn_59)
	{
		this.bn_59 = bn_59;
	}

}
