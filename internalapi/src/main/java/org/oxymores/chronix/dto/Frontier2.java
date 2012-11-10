package org.oxymores.chronix.dto;

import java.util.UUID;

import org.oxymores.chronix.core.active.ClockRRule;

public class Frontier2
{	
	public static ClockRRule getRRule(DTORRule r)
	{
		ClockRRule res = new ClockRRule();
		res.setId(UUID.fromString(r.id));
		res.setName(r.name);
		res.setDescription(r.description);

		res.setPeriod(r.period);
		res.setINTERVAL(r.interval);

		// ByDay
		String BD = "";
		if (r.bd_01)
			BD += "MO,";
		if (r.bd_02)
			BD += "TU,";
		if (r.bd_03)
			BD += "WE,";
		if (r.bd_04)
			BD += "TH,";
		if (r.bd_05)
			BD += "FR,";
		if (r.bd_06)
			BD += "SA,";
		if (r.bd_07)
			BD += "SU,";
		res.setBYDAY(BD);

		// ByMonthDay
		String BMD = "";
		if (r.bmd_01)
			BMD += "01,";
		if (r.bmdn_01)
			BMD += "-01,";
		if (r.bmd_02)
			BMD += "02,";
		if (r.bmdn_02)
			BMD += "-02,";
		if (r.bmd_03)
			BMD += "03,";
		if (r.bmdn_03)
			BMD += "-03,";
		if (r.bmd_04)
			BMD += "04,";
		if (r.bmdn_04)
			BMD += "-04,";
		if (r.bmd_05)
			BMD += "05,";
		if (r.bmdn_05)
			BMD += "-05,";
		if (r.bmd_06)
			BMD += "06,";
		if (r.bmdn_06)
			BMD += "-06,";
		if (r.bmd_07)
			BMD += "07,";
		if (r.bmdn_07)
			BMD += "-07,";
		if (r.bmd_08)
			BMD += "08,";
		if (r.bmdn_08)
			BMD += "-08,";
		if (r.bmd_09)
			BMD += "09,";
		if (r.bmdn_09)
			BMD += "-09,";
		if (r.bmd_10)
			BMD += "10,";
		if (r.bmdn_10)
			BMD += "-10,";
		if (r.bmd_11)
			BMD += "11,";
		if (r.bmdn_11)
			BMD += "-11,";
		if (r.bmd_12)
			BMD += "12,";
		if (r.bmdn_12)
			BMD += "-12,";
		if (r.bmd_13)
			BMD += "13,";
		if (r.bmdn_13)
			BMD += "-13,";
		if (r.bmd_14)
			BMD += "14,";
		if (r.bmdn_14)
			BMD += "-14,";
		if (r.bmd_15)
			BMD += "15,";
		if (r.bmdn_15)
			BMD += "-15,";
		if (r.bmd_16)
			BMD += "16,";
		if (r.bmdn_16)
			BMD += "-16,";
		if (r.bmd_17)
			BMD += "17,";
		if (r.bmdn_17)
			BMD += "-17,";
		if (r.bmd_18)
			BMD += "18,";
		if (r.bmdn_18)
			BMD += "-18,";
		if (r.bmd_19)
			BMD += "19,";
		if (r.bmdn_19)
			BMD += "-19,";
		if (r.bmd_20)
			BMD += "20,";
		if (r.bmdn_20)
			BMD += "-20,";
		if (r.bmd_21)
			BMD += "21,";
		if (r.bmdn_21)
			BMD += "-21,";
		if (r.bmd_22)
			BMD += "22,";
		if (r.bmdn_22)
			BMD += "-22,";
		if (r.bmd_23)
			BMD += "23,";
		if (r.bmdn_23)
			BMD += "-23,";
		if (r.bmd_24)
			BMD += "24,";
		if (r.bmdn_24)
			BMD += "-24,";
		if (r.bmd_25)
			BMD += "25,";
		if (r.bmdn_25)
			BMD += "-25,";
		if (r.bmd_26)
			BMD += "26,";
		if (r.bmdn_26)
			BMD += "-26,";
		if (r.bmd_27)
			BMD += "27,";
		if (r.bmdn_27)
			BMD += "-27,";
		if (r.bmd_28)
			BMD += "28,";
		if (r.bmdn_28)
			BMD += "-28,";
		if (r.bmd_29)
			BMD += "29,";
		if (r.bmdn_29)
			BMD += "-29,";
		if (r.bmd_30)
			BMD += "30,";
		if (r.bmdn_30)
			BMD += "-30,";
		if (r.bmd_31)
			BMD += "31,";
		if (r.bmdn_31)
			BMD += "-31,";
		res.setBYMONTHDAY(BMD);

		// ByMonth
		String BM = "";
		if (r.bm_01)
			BM += "01,";
		if (r.bm_02)
			BM += "02,";
		if (r.bm_03)
			BM += "03,";
		if (r.bm_04)
			BM += "04,";
		if (r.bm_05)
			BM += "05,";
		if (r.bm_06)
			BM += "06,";
		if (r.bm_07)
			BM += "07,";
		if (r.bm_08)
			BM += "08,";
		if (r.bm_09)
			BM += "09,";
		if (r.bm_10)
			BM += "10,";
		if (r.bm_11)
			BM += "11,";
		if (r.bm_12)
			BM += "12,";
		res.setBYMONTH(BM);

		// ByHour
		String BH = "";
		if (r.bh_00)
			BH += "00,";
		if (r.bh_01)
			BH += "01,";
		if (r.bh_02)
			BH += "02,";
		if (r.bh_03)
			BH += "03,";
		if (r.bh_04)
			BH += "04,";
		if (r.bh_05)
			BH += "05,";
		if (r.bh_06)
			BH += "06,";
		if (r.bh_07)
			BH += "07,";
		if (r.bh_08)
			BH += "08,";
		if (r.bh_09)
			BH += "09,";
		if (r.bh_10)
			BH += "10,";
		if (r.bh_11)
			BH += "11,";
		if (r.bh_12)
			BH += "12,";
		if (r.bh_13)
			BH += "13,";
		if (r.bh_14)
			BH += "14,";
		if (r.bh_15)
			BH += "15,";
		if (r.bh_16)
			BH += "16,";
		if (r.bh_17)
			BH += "17,";
		if (r.bh_18)
			BH += "18,";
		if (r.bh_19)
			BH += "19,";
		if (r.bh_20)
			BH += "20,";
		if (r.bh_21)
			BH += "21,";
		if (r.bh_22)
			BH += "22,";
		if (r.bh_23)
			BH += "23,";
		
		res.setBYHOUR(BH);

		// ByMinute
		String BN = "";
		if (r.bn_00)
			BN += "00,";
		if (r.bn_01)
			BN += "01,";
		if (r.bn_02)
			BN += "02,";
		if (r.bn_03)
			BN += "03,";
		if (r.bn_04)
			BN += "04,";
		if (r.bn_05)
			BN += "05,";
		if (r.bn_06)
			BN += "06,";
		if (r.bn_07)
			BN += "07,";
		if (r.bn_08)
			BN += "08,";
		if (r.bn_09)
			BN += "09,";
		if (r.bn_10)
			BN += "10,";
		if (r.bn_11)
			BN += "11,";
		if (r.bn_12)
			BN += "12,";
		if (r.bn_13)
			BN += "13,";
		if (r.bn_14)
			BN += "14,";
		if (r.bn_15)
			BN += "15,";
		if (r.bn_16)
			BN += "16,";
		if (r.bn_17)
			BN += "17,";
		if (r.bn_18)
			BN += "18,";
		if (r.bn_19)
			BN += "19,";
		if (r.bn_20)
			BN += "20,";
		if (r.bn_21)
			BN += "21,";
		if (r.bn_22)
			BN += "22,";
		if (r.bn_23)
			BN += "23,";
		if (r.bn_24)
			BN += "24,";
		if (r.bn_25)
			BN += "25,";
		if (r.bn_26)
			BN += "26,";
		if (r.bn_27)
			BN += "27,";
		if (r.bn_28)
			BN += "28,";
		if (r.bn_29)
			BN += "29,";
		if (r.bn_30)
			BN += "30,";
		if (r.bn_31)
			BN += "31,";
		if (r.bn_32)
			BN += "32,";
		if (r.bn_33)
			BN += "33,";
		if (r.bn_34)
			BN += "34,";
		if (r.bn_35)
			BN += "35,";
		if (r.bn_36)
			BN += "36,";
		if (r.bn_37)
			BN += "37,";
		if (r.bn_38)
			BN += "38,";
		if (r.bn_39)
			BN += "39,";
		if (r.bn_40)
			BN += "40,";
		if (r.bn_41)
			BN += "41,";
		if (r.bn_42)
			BN += "42,";
		if (r.bn_43)
			BN += "43,";
		if (r.bn_44)
			BN += "44,";
		if (r.bn_45)
			BN += "45,";
		if (r.bn_46)
			BN += "46,";
		if (r.bn_47)
			BN += "47,";
		if (r.bn_48)
			BN += "48,";
		if (r.bn_49)
			BN += "49,";
		if (r.bn_50)
			BN += "50,";
		if (r.bn_51)
			BN += "51,";
		if (r.bn_52)
			BN += "52,";
		if (r.bn_53)
			BN += "53,";
		if (r.bn_54)
			BN += "54,";
		if (r.bn_55)
			BN += "55,";
		if (r.bn_56)
			BN += "56,";
		if (r.bn_57)
			BN += "57,";
		if (r.bn_58)
			BN += "58,";
		if (r.bn_59)
			BN += "59,";
		res.setBYMINUTE(BN);

		return res;
	}
}
