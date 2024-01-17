{
    //obtains from the session all the useful user information
    var user = JSON.parse(sessionStorage.getItem('user'));

    //creates the page orchestrator
    var pageOrchestrator = new PageOrchestrator();// main controller
    var self= this;

    /*checks if the session is still active
    * in case if it isn't it shows the HomePage*/
    window.addEventListener("load", () => {
        if (user == null) {
            window.location.href = "HomePage.html";
        } else {
            //pageOrchestrator.start(); // initialize the components
        } // display initial content
    }, false);

    //It manages the interaction between all the different page
    function PageOrchestrator(){
        let goodMorningUser = document.getElementById("goodMorningUser");
        let goToUserHomePage =document.getElementById("goToUserHomePage")
        let error= document.getElementById("error")
        let errormessage= document.getElementById("errormessage")
        let rollback =document.getElementById("rollback")

        let personalHomePage = new PersonalHomePage(user);
        let tournamentPage = new TournamentPage(user);
        let battlePage= new BattlePage(user)
        let actualPage= personalHomePage
        goodMorningUser.innerHTML="Goodmorning, " + user.username + "!"
        goToUserHomePage.addEventListener("click", (e) => {
            actualPage.hide()
            personalHomePage.openPage()
            actualPage= personalHomePage
        }, false);
        personalHomePage.openPage()

        /*This function is used to show the tournament page
        * It updates the actual and then calls the openpage function*/
        this.showTournamentPage = function (id) {
            actualPage = tournamentPage
            tournamentPage.openPage(id)
        };

        /*This function is used to show the error page
        * In particular it hides the actual page, and the shows the error message
        * that has just received with a button that allows to come back to the previous page*/
        this.showError=function (message){
            actualPage.hide()
            error.style.display=""
            errormessage.innerHTML=message
            rollback.addEventListener("click",(e)=>{
                    error.style.display="none"
                    actualPage.openPage()
                }
            )
        }
    }
}