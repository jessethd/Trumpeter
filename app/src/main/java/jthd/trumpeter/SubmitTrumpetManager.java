package jthd.trumpeter;


import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;



public final class SubmitTrumpetManager {

    private static int mLastTrumpetID;

    private SubmitTrumpetManager(){

    }

    /**
     * Creates a new ParseObject of class "Trumpet" with the user-provided information and submitting ParseUser. Submission date automatically saved in
     * "createdAt" field.
     */
    public static void submitNewTrumpet(String text, ParseUser user){
        ParseObject trumpet = new ParseObject("Trumpet");
        trumpet.put("text", text);
        trumpet.put("user", user);
        trumpet.put("retrumpet", false);
        trumpet.put("retrumpeter", null);
        trumpet.put("retrumpets", 0);
        trumpet.put("likes", 0);
        trumpet.put("trumpetID", SubmitTrumpetManager.getNextTrumpetID());
        trumpet.saveInBackground();
    }

    public static void submitRetrumpet(ParseObject trumpet, String retrumpeter){
        SubmitTrumpetManager.updateRetrumpetCount(trumpet.getInt("trumpetID"));
        ParseObject retrumpet = new ParseObject("Trumpet");
        trumpet.put("text", trumpet.get("text"));
        trumpet.put("user", trumpet.get("user"));
        trumpet.put("retrumpet", true);
        trumpet.put("retrumpeter", retrumpeter);
        trumpet.put("retrumpets", trumpet.get("retrumpets"));
        trumpet.put("likes", trumpet.get("likes"));
        trumpet.put("trumpetID", trumpet.get("trumpetID"));
        trumpet.saveInBackground();
    }



    /**
    * Retrieves the next available Trumpet ID for a new Trumpet from the TrumpetCounter object and atomically increments the counter field.
    * @return Returns the first available Trumpet ID (max + 1).
    */
    private static int getNextTrumpetID(){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("TrumpetCounter");
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject trumpetCounter, ParseException e) {
                if (e == null) {
                    mLastTrumpetID = trumpetCounter.getInt("nextTrumpetID");
                    trumpetCounter.increment("nextTrumpetID");
                    trumpetCounter.saveInBackground();
                } else {

                }
            }
        });
        return mLastTrumpetID;
    }

    // TODO This really ought to be a in an UpdateTrumpetManager class that has updateRetrumpet and updateLikes
    private static int updateRetrumpetCount(int trumpetID){
        // find all Trumpets with given ID and increment their "retrumpets" count. Still need to handle the +1 clientside when retrumpet button is pressed
    }



}