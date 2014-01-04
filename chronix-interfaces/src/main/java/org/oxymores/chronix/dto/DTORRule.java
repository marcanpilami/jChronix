package org.oxymores.chronix.dto;

public class DTORRule
{
    private String name;
    private String description;
    private String id;

    private String period;
    private int interval;

    // ByDay (01 = Monday, 07 = Sunday)
    private boolean bd01 = false, bd02 = false, bd03 = false, bd04 = false, bd05 = false, bd06 = false, bd07 = false;
    // ByMonthDay
    private boolean bmd01 = false, bmd02 = false, bmd03 = false, bmd04 = false, bmd05 = false, bmd06 = false, bmd07 = false,
            bmd08 = false, bmd09 = false, bmd10 = false, bmd11 = false, bmd12 = false, bmd13 = false, bmd14 = false, bmd15 = false,
            bmd16 = false, bmd17 = false, bmd18 = false, bmd19 = false, bmd20 = false, bmd21 = false, bmd22 = false, bmd23 = false,
            bmd24 = false, bmd25 = false, bmd26 = false, bmd27 = false, bmd28 = false, bmd29 = false, bmd30 = false, bmd31 = false;
    private boolean bmdn01 = false, bmdn02 = false, bmdn03 = false, bmdn04 = false, bmdn05 = false, bmdn06 = false, bmdn07 = false,
            bmdn08 = false, bmdn09 = false, bmdn10 = false, bmdn11 = false, bmdn12 = false, bmdn13 = false, bmdn14 = false,
            bmdn15 = false, bmdn16 = false, bmdn17 = false, bmdn18 = false, bmdn19 = false, bmdn20 = false, bmdn21 = false,
            bmdn22 = false, bmdn23 = false, bmdn24 = false, bmdn25 = false, bmdn26 = false, bmdn27 = false, bmdn28 = false,
            bmdn29 = false, bmdn30 = false, bmdn31 = false;
    // ByMonth (01 = January)
    private boolean bm01 = false, bm02 = false, bm03 = false, bm04 = false, bm05 = false, bm06 = false, bm07 = false, bm08 = false,
            bm09 = false, bm10 = false, bm11 = false, bm12 = false;
    // ByHour
    private boolean bh00 = false, bh01 = false, bh02 = false, bh03 = false, bh04 = false, bh05 = false, bh06 = false, bh07 = false,
            bh08 = false, bh09 = false, bh10 = false, bh11 = false, bh12 = false, bh13 = false, bh14 = false, bh15 = false,
            bh16 = false, bh17 = false, bh18 = false, bh19 = false, bh20 = false, bh21 = false, bh22 = false, bh23 = false;
    // ByMinute
    private boolean bn00 = false, bn01 = false, bn02 = false, bn03 = false, bn04 = false, bn05 = false, bn06 = false, bn07 = false,
            bn08 = false, bn09 = false, bn10 = false, bn11 = false, bn12 = false, bn13 = false, bn14 = false, bn15 = false,
            bn16 = false, bn17 = false, bn18 = false, bn19 = false, bn20 = false, bn21 = false, bn22 = false, bn23 = false,
            bn24 = false, bn25 = false, bn26 = false, bn27 = false, bn28 = false, bn29 = false, bn30 = false, bn31 = false,
            bn32 = false, bn33 = false, bn34 = false, bn35 = false, bn36 = false, bn37 = false, bn38 = false, bn39 = false,
            bn40 = false, bn41 = false, bn42 = false, bn43 = false, bn44 = false, bn45 = false, bn46 = false, bn47 = false,
            bn48 = false, bn49 = false, bn50 = false, bn51 = false, bn52 = false, bn53 = false, bn54 = false, bn55 = false,
            bn56 = false, bn57 = false, bn58 = false, bn59 = false;

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

    public boolean isBmd01()
    {
        return bmd01;
    }

    public void setBmd01(boolean bmd01)
    {
        this.bmd01 = bmd01;
    }

    public boolean isBmd02()
    {
        return bmd02;
    }

    public void setBmd02(boolean bmd02)
    {
        this.bmd02 = bmd02;
    }

    public boolean isBmd03()
    {
        return bmd03;
    }

    public void setBmd03(boolean bmd03)
    {
        this.bmd03 = bmd03;
    }

    public boolean isBmd04()
    {
        return bmd04;
    }

    public void setBmd04(boolean bmd04)
    {
        this.bmd04 = bmd04;
    }

    public boolean isBmd05()
    {
        return bmd05;
    }

    public void setBmd05(boolean bmd05)
    {
        this.bmd05 = bmd05;
    }

    public boolean isBmd06()
    {
        return bmd06;
    }

    public void setBmd06(boolean bmd06)
    {
        this.bmd06 = bmd06;
    }

    public boolean isBmd07()
    {
        return bmd07;
    }

    public void setBmd07(boolean bmd07)
    {
        this.bmd07 = bmd07;
    }

    public boolean isBmd08()
    {
        return bmd08;
    }

    public void setBmd08(boolean bmd08)
    {
        this.bmd08 = bmd08;
    }

    public boolean isBmd09()
    {
        return bmd09;
    }

    public void setBmd09(boolean bmd09)
    {
        this.bmd09 = bmd09;
    }

    public boolean isBmd10()
    {
        return bmd10;
    }

    public void setBmd10(boolean bmd10)
    {
        this.bmd10 = bmd10;
    }

    public boolean isBmd11()
    {
        return bmd11;
    }

    public void setBmd11(boolean bmd11)
    {
        this.bmd11 = bmd11;
    }

    public boolean isBmd12()
    {
        return bmd12;
    }

    public void setBmd12(boolean bmd12)
    {
        this.bmd12 = bmd12;
    }

    public boolean isBmd13()
    {
        return bmd13;
    }

    public void setBmd13(boolean bmd13)
    {
        this.bmd13 = bmd13;
    }

    public boolean isBmd14()
    {
        return bmd14;
    }

    public void setBmd14(boolean bmd14)
    {
        this.bmd14 = bmd14;
    }

    public boolean isBmd15()
    {
        return bmd15;
    }

    public void setBmd15(boolean bmd15)
    {
        this.bmd15 = bmd15;
    }

    public boolean isBmd16()
    {
        return bmd16;
    }

    public void setBmd16(boolean bmd16)
    {
        this.bmd16 = bmd16;
    }

    public boolean isBmd17()
    {
        return bmd17;
    }

    public void setBmd17(boolean bmd17)
    {
        this.bmd17 = bmd17;
    }

    public boolean isBmd18()
    {
        return bmd18;
    }

    public void setBmd18(boolean bmd18)
    {
        this.bmd18 = bmd18;
    }

    public boolean isBmd19()
    {
        return bmd19;
    }

    public void setBmd19(boolean bmd19)
    {
        this.bmd19 = bmd19;
    }

    public boolean isBmd20()
    {
        return bmd20;
    }

    public void setBmd20(boolean bmd20)
    {
        this.bmd20 = bmd20;
    }

    public boolean isBmd21()
    {
        return bmd21;
    }

    public void setBmd21(boolean bmd21)
    {
        this.bmd21 = bmd21;
    }

    public boolean isBmd22()
    {
        return bmd22;
    }

    public void setBmd22(boolean bmd22)
    {
        this.bmd22 = bmd22;
    }

    public boolean isBmd23()
    {
        return bmd23;
    }

    public void setBmd23(boolean bmd23)
    {
        this.bmd23 = bmd23;
    }

    public boolean isBmd24()
    {
        return bmd24;
    }

    public void setBmd24(boolean bmd24)
    {
        this.bmd24 = bmd24;
    }

    public boolean isBmd25()
    {
        return bmd25;
    }

    public void setBmd25(boolean bmd25)
    {
        this.bmd25 = bmd25;
    }

    public boolean isBmd26()
    {
        return bmd26;
    }

    public void setBmd26(boolean bmd26)
    {
        this.bmd26 = bmd26;
    }

    public boolean isBmd27()
    {
        return bmd27;
    }

    public void setBmd27(boolean bmd27)
    {
        this.bmd27 = bmd27;
    }

    public boolean isBmd28()
    {
        return bmd28;
    }

    public void setBmd28(boolean bmd28)
    {
        this.bmd28 = bmd28;
    }

    public boolean isBmd29()
    {
        return bmd29;
    }

    public void setBmd29(boolean bmd29)
    {
        this.bmd29 = bmd29;
    }

    public boolean isBmd30()
    {
        return bmd30;
    }

    public void setBmd30(boolean bmd30)
    {
        this.bmd30 = bmd30;
    }

    public boolean isBmd31()
    {
        return bmd31;
    }

    public void setBmd31(boolean bmd31)
    {
        this.bmd31 = bmd31;
    }

    public boolean isBmdn01()
    {
        return bmdn01;
    }

    public void setBmdn01(boolean bmdn01)
    {
        this.bmdn01 = bmdn01;
    }

    public boolean isBmdn02()
    {
        return bmdn02;
    }

    public void setBmdn02(boolean bmdn02)
    {
        this.bmdn02 = bmdn02;
    }

    public boolean isBmdn03()
    {
        return bmdn03;
    }

    public void setBmdn03(boolean bmdn03)
    {
        this.bmdn03 = bmdn03;
    }

    public boolean isBmdn04()
    {
        return bmdn04;
    }

    public void setBmdn04(boolean bmdn04)
    {
        this.bmdn04 = bmdn04;
    }

    public boolean isBmdn05()
    {
        return bmdn05;
    }

    public void setBmdn05(boolean bmdn05)
    {
        this.bmdn05 = bmdn05;
    }

    public boolean isBmdn06()
    {
        return bmdn06;
    }

    public void setBmdn06(boolean bmdn06)
    {
        this.bmdn06 = bmdn06;
    }

    public boolean isBmdn07()
    {
        return bmdn07;
    }

    public void setBmdn07(boolean bmdn07)
    {
        this.bmdn07 = bmdn07;
    }

    public boolean isBmdn08()
    {
        return bmdn08;
    }

    public void setBmdn08(boolean bmdn08)
    {
        this.bmdn08 = bmdn08;
    }

    public boolean isBmdn09()
    {
        return bmdn09;
    }

    public void setBmdn09(boolean bmdn09)
    {
        this.bmdn09 = bmdn09;
    }

    public boolean isBmdn10()
    {
        return bmdn10;
    }

    public void setBmdn10(boolean bmdn10)
    {
        this.bmdn10 = bmdn10;
    }

    public boolean isBmdn11()
    {
        return bmdn11;
    }

    public void setBmdn11(boolean bmdn11)
    {
        this.bmdn11 = bmdn11;
    }

    public boolean isBmdn12()
    {
        return bmdn12;
    }

    public void setBmdn12(boolean bmdn12)
    {
        this.bmdn12 = bmdn12;
    }

    public boolean isBmdn13()
    {
        return bmdn13;
    }

    public void setBmdn13(boolean bmdn13)
    {
        this.bmdn13 = bmdn13;
    }

    public boolean isBmdn14()
    {
        return bmdn14;
    }

    public void setBmdn14(boolean bmdn14)
    {
        this.bmdn14 = bmdn14;
    }

    public boolean isBmdn15()
    {
        return bmdn15;
    }

    public void setBmdn15(boolean bmdn15)
    {
        this.bmdn15 = bmdn15;
    }

    public boolean isBmdn16()
    {
        return bmdn16;
    }

    public void setBmdn16(boolean bmdn16)
    {
        this.bmdn16 = bmdn16;
    }

    public boolean isBmdn17()
    {
        return bmdn17;
    }

    public void setBmdn17(boolean bmdn17)
    {
        this.bmdn17 = bmdn17;
    }

    public boolean isBmdn18()
    {
        return bmdn18;
    }

    public void setBmdn18(boolean bmdn18)
    {
        this.bmdn18 = bmdn18;
    }

    public boolean isBmdn19()
    {
        return bmdn19;
    }

    public void setBmdn19(boolean bmdn19)
    {
        this.bmdn19 = bmdn19;
    }

    public boolean isBmdn20()
    {
        return bmdn20;
    }

    public void setBmdn20(boolean bmdn20)
    {
        this.bmdn20 = bmdn20;
    }

    public boolean isBmdn21()
    {
        return bmdn21;
    }

    public void setBmdn21(boolean bmdn21)
    {
        this.bmdn21 = bmdn21;
    }

    public boolean isBmdn22()
    {
        return bmdn22;
    }

    public void setBmdn22(boolean bmdn22)
    {
        this.bmdn22 = bmdn22;
    }

    public boolean isBmdn23()
    {
        return bmdn23;
    }

    public void setBmdn23(boolean bmdn23)
    {
        this.bmdn23 = bmdn23;
    }

    public boolean isBmdn24()
    {
        return bmdn24;
    }

    public void setBmdn24(boolean bmdn24)
    {
        this.bmdn24 = bmdn24;
    }

    public boolean isBmdn25()
    {
        return bmdn25;
    }

    public void setBmdn25(boolean bmdn25)
    {
        this.bmdn25 = bmdn25;
    }

    public boolean isBmdn26()
    {
        return bmdn26;
    }

    public void setBmdn26(boolean bmdn26)
    {
        this.bmdn26 = bmdn26;
    }

    public boolean isBmdn27()
    {
        return bmdn27;
    }

    public void setBmdn27(boolean bmdn27)
    {
        this.bmdn27 = bmdn27;
    }

    public boolean isBmdn28()
    {
        return bmdn28;
    }

    public void setBmdn28(boolean bmdn28)
    {
        this.bmdn28 = bmdn28;
    }

    public boolean isBmdn29()
    {
        return bmdn29;
    }

    public void setBmdn29(boolean bmdn29)
    {
        this.bmdn29 = bmdn29;
    }

    public boolean isBmdn30()
    {
        return bmdn30;
    }

    public void setBmdn30(boolean bmdn30)
    {
        this.bmdn30 = bmdn30;
    }

    public boolean isBmdn31()
    {
        return bmdn31;
    }

    public void setBmdn31(boolean bmdn31)
    {
        this.bmdn31 = bmdn31;
    }

    public boolean isBh00()
    {
        return bh00;
    }

    public void setBh00(boolean bh00)
    {
        this.bh00 = bh00;
    }

    public boolean isBh01()
    {
        return bh01;
    }

    public void setBh01(boolean bh01)
    {
        this.bh01 = bh01;
    }

    public boolean isBh02()
    {
        return bh02;
    }

    public void setBh02(boolean bh02)
    {
        this.bh02 = bh02;
    }

    public boolean isBh03()
    {
        return bh03;
    }

    public void setBh03(boolean bh03)
    {
        this.bh03 = bh03;
    }

    public boolean isBh04()
    {
        return bh04;
    }

    public void setBh04(boolean bh04)
    {
        this.bh04 = bh04;
    }

    public boolean isBh05()
    {
        return bh05;
    }

    public void setBh05(boolean bh05)
    {
        this.bh05 = bh05;
    }

    public boolean isBh06()
    {
        return bh06;
    }

    public void setBh06(boolean bh06)
    {
        this.bh06 = bh06;
    }

    public boolean isBh07()
    {
        return bh07;
    }

    public void setBh07(boolean bh07)
    {
        this.bh07 = bh07;
    }

    public boolean isBh08()
    {
        return bh08;
    }

    public void setBh08(boolean bh08)
    {
        this.bh08 = bh08;
    }

    public boolean isBh09()
    {
        return bh09;
    }

    public void setBh09(boolean bh09)
    {
        this.bh09 = bh09;
    }

    public boolean isBh10()
    {
        return bh10;
    }

    public void setBh10(boolean bh10)
    {
        this.bh10 = bh10;
    }

    public boolean isBh11()
    {
        return bh11;
    }

    public void setBh11(boolean bh11)
    {
        this.bh11 = bh11;
    }

    public boolean isBh12()
    {
        return bh12;
    }

    public void setBh12(boolean bh12)
    {
        this.bh12 = bh12;
    }

    public boolean isBh13()
    {
        return bh13;
    }

    public void setBh13(boolean bh13)
    {
        this.bh13 = bh13;
    }

    public boolean isBh14()
    {
        return bh14;
    }

    public void setBh14(boolean bh14)
    {
        this.bh14 = bh14;
    }

    public boolean isBh15()
    {
        return bh15;
    }

    public void setBh15(boolean bh15)
    {
        this.bh15 = bh15;
    }

    public boolean isBh16()
    {
        return bh16;
    }

    public void setBh16(boolean bh16)
    {
        this.bh16 = bh16;
    }

    public boolean isBh17()
    {
        return bh17;
    }

    public void setBh17(boolean bh17)
    {
        this.bh17 = bh17;
    }

    public boolean isBh18()
    {
        return bh18;
    }

    public void setBh18(boolean bh18)
    {
        this.bh18 = bh18;
    }

    public boolean isBh19()
    {
        return bh19;
    }

    public void setBh19(boolean bh19)
    {
        this.bh19 = bh19;
    }

    public boolean isBh20()
    {
        return bh20;
    }

    public void setBh20(boolean bh20)
    {
        this.bh20 = bh20;
    }

    public boolean isBh21()
    {
        return bh21;
    }

    public void setBh21(boolean bh21)
    {
        this.bh21 = bh21;
    }

    public boolean isBh22()
    {
        return bh22;
    }

    public void setBh22(boolean bh22)
    {
        this.bh22 = bh22;
    }

    public boolean isBh23()
    {
        return bh23;
    }

    public void setBh23(boolean bh23)
    {
        this.bh23 = bh23;
    }

    public boolean isBd01()
    {
        return bd01;
    }

    public void setBd01(boolean bd01)
    {
        this.bd01 = bd01;
    }

    public boolean isBd02()
    {
        return bd02;
    }

    public void setBd02(boolean bd02)
    {
        this.bd02 = bd02;
    }

    public boolean isBd03()
    {
        return bd03;
    }

    public void setBd03(boolean bd03)
    {
        this.bd03 = bd03;
    }

    public boolean isBd04()
    {
        return bd04;
    }

    public void setBd04(boolean bd04)
    {
        this.bd04 = bd04;
    }

    public boolean isBd05()
    {
        return bd05;
    }

    public void setBd05(boolean bd05)
    {
        this.bd05 = bd05;
    }

    public boolean isBd06()
    {
        return bd06;
    }

    public void setBd06(boolean bd06)
    {
        this.bd06 = bd06;
    }

    public boolean isBd07()
    {
        return bd07;
    }

    public void setBd07(boolean bd07)
    {
        this.bd07 = bd07;
    }

    public boolean isBm01()
    {
        return bm01;
    }

    public void setBm01(boolean bm01)
    {
        this.bm01 = bm01;
    }

    public boolean isBm02()
    {
        return bm02;
    }

    public void setBm02(boolean bm02)
    {
        this.bm02 = bm02;
    }

    public boolean isBm03()
    {
        return bm03;
    }

    public void setBm03(boolean bm03)
    {
        this.bm03 = bm03;
    }

    public boolean isBm04()
    {
        return bm04;
    }

    public void setBm04(boolean bm04)
    {
        this.bm04 = bm04;
    }

    public boolean isBm05()
    {
        return bm05;
    }

    public void setBm05(boolean bm05)
    {
        this.bm05 = bm05;
    }

    public boolean isBm06()
    {
        return bm06;
    }

    public void setBm06(boolean bm06)
    {
        this.bm06 = bm06;
    }

    public boolean isBm07()
    {
        return bm07;
    }

    public void setBm07(boolean bm07)
    {
        this.bm07 = bm07;
    }

    public boolean isBm08()
    {
        return bm08;
    }

    public void setBm08(boolean bm08)
    {
        this.bm08 = bm08;
    }

    public boolean isBm09()
    {
        return bm09;
    }

    public void setBm09(boolean bm09)
    {
        this.bm09 = bm09;
    }

    public boolean isBm10()
    {
        return bm10;
    }

    public void setBm10(boolean bm10)
    {
        this.bm10 = bm10;
    }

    public boolean isBm11()
    {
        return bm11;
    }

    public void setBm11(boolean bm11)
    {
        this.bm11 = bm11;
    }

    public boolean isBm12()
    {
        return bm12;
    }

    public void setBm12(boolean bm12)
    {
        this.bm12 = bm12;
    }

    public boolean isBn00()
    {
        return bn00;
    }

    public void setBn00(boolean bn00)
    {
        this.bn00 = bn00;
    }

    public boolean isBn01()
    {
        return bn01;
    }

    public void setBn01(boolean bn01)
    {
        this.bn01 = bn01;
    }

    public boolean isBn02()
    {
        return bn02;
    }

    public void setBn02(boolean bn02)
    {
        this.bn02 = bn02;
    }

    public boolean isBn03()
    {
        return bn03;
    }

    public void setBn03(boolean bn03)
    {
        this.bn03 = bn03;
    }

    public boolean isBn04()
    {
        return bn04;
    }

    public void setBn04(boolean bn04)
    {
        this.bn04 = bn04;
    }

    public boolean isBn05()
    {
        return bn05;
    }

    public void setBn05(boolean bn05)
    {
        this.bn05 = bn05;
    }

    public boolean isBn06()
    {
        return bn06;
    }

    public void setBn06(boolean bn06)
    {
        this.bn06 = bn06;
    }

    public boolean isBn07()
    {
        return bn07;
    }

    public void setBn07(boolean bn07)
    {
        this.bn07 = bn07;
    }

    public boolean isBn08()
    {
        return bn08;
    }

    public void setBn08(boolean bn08)
    {
        this.bn08 = bn08;
    }

    public boolean isBn09()
    {
        return bn09;
    }

    public void setBn09(boolean bn09)
    {
        this.bn09 = bn09;
    }

    public boolean isBn10()
    {
        return bn10;
    }

    public void setBn10(boolean bn10)
    {
        this.bn10 = bn10;
    }

    public boolean isBn11()
    {
        return bn11;
    }

    public void setBn11(boolean bn11)
    {
        this.bn11 = bn11;
    }

    public boolean isBn12()
    {
        return bn12;
    }

    public void setBn12(boolean bn12)
    {
        this.bn12 = bn12;
    }

    public boolean isBn13()
    {
        return bn13;
    }

    public void setBn13(boolean bn13)
    {
        this.bn13 = bn13;
    }

    public boolean isBn14()
    {
        return bn14;
    }

    public void setBn14(boolean bn14)
    {
        this.bn14 = bn14;
    }

    public boolean isBn15()
    {
        return bn15;
    }

    public void setBn15(boolean bn15)
    {
        this.bn15 = bn15;
    }

    public boolean isBn16()
    {
        return bn16;
    }

    public void setBn16(boolean bn16)
    {
        this.bn16 = bn16;
    }

    public boolean isBn17()
    {
        return bn17;
    }

    public void setBn17(boolean bn17)
    {
        this.bn17 = bn17;
    }

    public boolean isBn18()
    {
        return bn18;
    }

    public void setBn18(boolean bn18)
    {
        this.bn18 = bn18;
    }

    public boolean isBn19()
    {
        return bn19;
    }

    public void setBn19(boolean bn19)
    {
        this.bn19 = bn19;
    }

    public boolean isBn20()
    {
        return bn20;
    }

    public void setBn20(boolean bn20)
    {
        this.bn20 = bn20;
    }

    public boolean isBn21()
    {
        return bn21;
    }

    public void setBn21(boolean bn21)
    {
        this.bn21 = bn21;
    }

    public boolean isBn22()
    {
        return bn22;
    }

    public void setBn22(boolean bn22)
    {
        this.bn22 = bn22;
    }

    public boolean isBn23()
    {
        return bn23;
    }

    public void setBn23(boolean bn23)
    {
        this.bn23 = bn23;
    }

    public boolean isBn24()
    {
        return bn24;
    }

    public void setBn24(boolean bn24)
    {
        this.bn24 = bn24;
    }

    public boolean isBn25()
    {
        return bn25;
    }

    public void setBn25(boolean bn25)
    {
        this.bn25 = bn25;
    }

    public boolean isBn26()
    {
        return bn26;
    }

    public void setBn26(boolean bn26)
    {
        this.bn26 = bn26;
    }

    public boolean isBn27()
    {
        return bn27;
    }

    public void setBn27(boolean bn27)
    {
        this.bn27 = bn27;
    }

    public boolean isBn28()
    {
        return bn28;
    }

    public void setBn28(boolean bn28)
    {
        this.bn28 = bn28;
    }

    public boolean isBn29()
    {
        return bn29;
    }

    public void setBn29(boolean bn29)
    {
        this.bn29 = bn29;
    }

    public boolean isBn30()
    {
        return bn30;
    }

    public void setBn30(boolean bn30)
    {
        this.bn30 = bn30;
    }

    public boolean isBn31()
    {
        return bn31;
    }

    public void setBn31(boolean bn31)
    {
        this.bn31 = bn31;
    }

    public boolean isBn32()
    {
        return bn32;
    }

    public void setBn32(boolean bn32)
    {
        this.bn32 = bn32;
    }

    public boolean isBn33()
    {
        return bn33;
    }

    public void setBn33(boolean bn33)
    {
        this.bn33 = bn33;
    }

    public boolean isBn34()
    {
        return bn34;
    }

    public void setBn34(boolean bn34)
    {
        this.bn34 = bn34;
    }

    public boolean isBn35()
    {
        return bn35;
    }

    public void setBn35(boolean bn35)
    {
        this.bn35 = bn35;
    }

    public boolean isBn36()
    {
        return bn36;
    }

    public void setBn36(boolean bn36)
    {
        this.bn36 = bn36;
    }

    public boolean isBn37()
    {
        return bn37;
    }

    public void setBn37(boolean bn37)
    {
        this.bn37 = bn37;
    }

    public boolean isBn38()
    {
        return bn38;
    }

    public void setBn38(boolean bn38)
    {
        this.bn38 = bn38;
    }

    public boolean isBn39()
    {
        return bn39;
    }

    public void setBn39(boolean bn39)
    {
        this.bn39 = bn39;
    }

    public boolean isBn40()
    {
        return bn40;
    }

    public void setBn40(boolean bn40)
    {
        this.bn40 = bn40;
    }

    public boolean isBn41()
    {
        return bn41;
    }

    public void setBn41(boolean bn41)
    {
        this.bn41 = bn41;
    }

    public boolean isBn42()
    {
        return bn42;
    }

    public void setBn42(boolean bn42)
    {
        this.bn42 = bn42;
    }

    public boolean isBn43()
    {
        return bn43;
    }

    public void setBn43(boolean bn43)
    {
        this.bn43 = bn43;
    }

    public boolean isBn44()
    {
        return bn44;
    }

    public void setBn44(boolean bn44)
    {
        this.bn44 = bn44;
    }

    public boolean isBn45()
    {
        return bn45;
    }

    public void setBn45(boolean bn45)
    {
        this.bn45 = bn45;
    }

    public boolean isBn46()
    {
        return bn46;
    }

    public void setBn46(boolean bn46)
    {
        this.bn46 = bn46;
    }

    public boolean isBn47()
    {
        return bn47;
    }

    public void setBn47(boolean bn47)
    {
        this.bn47 = bn47;
    }

    public boolean isBn48()
    {
        return bn48;
    }

    public void setBn48(boolean bn48)
    {
        this.bn48 = bn48;
    }

    public boolean isBn49()
    {
        return bn49;
    }

    public void setBn49(boolean bn49)
    {
        this.bn49 = bn49;
    }

    public boolean isBn50()
    {
        return bn50;
    }

    public void setBn50(boolean bn50)
    {
        this.bn50 = bn50;
    }

    public boolean isBn51()
    {
        return bn51;
    }

    public void setBn51(boolean bn51)
    {
        this.bn51 = bn51;
    }

    public boolean isBn52()
    {
        return bn52;
    }

    public void setBn52(boolean bn52)
    {
        this.bn52 = bn52;
    }

    public boolean isBn53()
    {
        return bn53;
    }

    public void setBn53(boolean bn53)
    {
        this.bn53 = bn53;
    }

    public boolean isBn54()
    {
        return bn54;
    }

    public void setBn54(boolean bn54)
    {
        this.bn54 = bn54;
    }

    public boolean isBn55()
    {
        return bn55;
    }

    public void setBn55(boolean bn55)
    {
        this.bn55 = bn55;
    }

    public boolean isBn56()
    {
        return bn56;
    }

    public void setBn56(boolean bn56)
    {
        this.bn56 = bn56;
    }

    public boolean isBn57()
    {
        return bn57;
    }

    public void setBn57(boolean bn57)
    {
        this.bn57 = bn57;
    }

    public boolean isBn58()
    {
        return bn58;
    }

    public void setBn58(boolean bn58)
    {
        this.bn58 = bn58;
    }

    public boolean isBn59()
    {
        return bn59;
    }

    public void setBn59(boolean bn59)
    {
        this.bn59 = bn59;
    }

}
