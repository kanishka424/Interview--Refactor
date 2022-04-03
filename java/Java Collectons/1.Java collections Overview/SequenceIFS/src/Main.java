public class Main {

    public static void main(String[] args) {
        System.out.println(sequencerIncrement("CMB001"));


    }

    public static String sequencerIncrement(String stringId) {

        int indexOfFirstDigit=0;
        String prefix;
        String digitPart;
        int digitPartInteger;
        int digitPartPlusOneInteger;
        String digitPartPlusOne;



        for (int i = 0; i < stringId.length() - 1; i++) {
            if (stringId.charAt(i) >= '0'
                    && stringId.charAt(i) <= '9') {
                indexOfFirstDigit=i;
            }
        }

        prefix=stringId.substring(0,indexOfFirstDigit);
        digitPart=stringId.substring(indexOfFirstDigit);
        int lengthOfDigitPart=digitPart.length();

        digitPartInteger=Integer.parseInt(digitPart);
        digitPartPlusOneInteger=digitPartInteger+1;
        digitPartPlusOne=String.valueOf(digitPartPlusOneInteger);
        System.out.println("digitPartPlusOne"+digitPartPlusOne);
        int lengthOfDigitPartPlusOne=digitPartPlusOne.length();

        if(lengthOfDigitPartPlusOne!=lengthOfDigitPart){
            int j=lengthOfDigitPart;
            digitPartPlusOne="";
            while(j!=0) {
                digitPartPlusOne +="0";
                        j--;
            }
        }




        return prefix+digitPartPlusOne;








    }
}
