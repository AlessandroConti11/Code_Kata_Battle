(function() { // avoid variables ending up in the global scope

    document.getElementById("loginsubmit").addEventListener('click', (e) => {
        var form = e.target.closest("form")
        if (form.checkValidity()) {
            makeCall("POST", 'LoginManager', e.target.closest("form"),
                function(x) {
                    if (x.readyState === XMLHttpRequest.DONE) {
                        var message = x.responseText;
                        switch (x.status) {
                            case 200:
                                sessionStorage.setItem('username',message);
                                window.location.href = "StudentHomePage.html";
                                break;
                            case 400: // bad request
                                document.getElementById("errormessage").textContent = message;
                                break;
                            case 401: // unauthorized
                                document.getElementById("errormessage").textContent = message;
                                break;
                            case 500: // server error
                                document.getElementById("errormessage").textContent = message;
                                break;
                        }
                    }
                }
            );
        } else {
            form.reportValidity();
        }
        clearForm("loginForm")
    });

    document.getElementById("signinsubmit").addEventListener('click', (e) => {
        var form = e.target.closest("form")
        if (form.checkValidity()) {
            makeCall("POST", 'SignInManager', e.target.closest("form"),
                function(x) {
                    if (x.readyState === XMLHttpRequest.DONE) {
                        var message = x.responseText;
                        switch (x.status) {
                            case 200:
                                console.log("ciao")
                                closeModal()
                                console.log("ciao")
                                openModal("otherInformation")
                                console.log("ciao")
                                let p= document.createElement("p")
                                let p1= document.createElement("p")
                                let signinmessage= document.getElementById("otherInformation")
                                console.log("ciao")
                                p.textContent="We are sending you a confirmation email"
                                signinmessage.append(p);
                                p1.textContent="Please check your email"
                                signinmessage.append(p1);
                                console.log("ciao")
                                break;
                            case 400: // bad request
                                document.getElementById("errormessagesignin").textContent = message;
                                break;
                            case 401: // unauthorized
                                document.getElementById("errormessagesignin").textContent = message;
                                break;
                            case 500: // server error
                                document.getElementById("errormessagesignin").textContent = message;
                                break;
                        }
                    }
                }
            );
            clearForm("signinForm")
        } else {
            form.reportValidity();
        }
    });
})();