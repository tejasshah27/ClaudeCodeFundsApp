# Fund Dashboard Project

## Business Requiremenets:
1. Fund dashboard web app should be able to render Funds related data
2. The dashboard should support multi role login.
3. Different user roles supported should be 'Read Only User' and 'Approver' and 'Editor'.
4. Upon Editor or Read Only User login, the dashboard should list of all available funds in the system but each row having only minimum number of columns. ( total possible data elements against any fund is 20 off which dashboard rows show only 5 )
5. Upon Approver login, the dashboard should list of all available funds in the system but each row having all columns.
6. Funds Details page ( Read Only user behavior ) : Read Only user could only click each row and it should pop open the funds detail in separate page showing up all 20 attributes that are there in system against each fund. The page should have only Close option.
7. Funds Details page ( Editor role behavior ) : Editor could click any of the row on dashboard , the system should open the full details of the particular fund in a separate page as a pop up.
8. Editor gets Save / Sumbit / Close action buttons on Funds details page.
9. Funds Details page ( Approver role behavior ) : Approver could click any of the row on dashboard , the system should open the full details of the particular fund in a separate page as a pop up.
10. Approver gets Approve / Reject / Close action buttons on Funds details page.

## Scope and limitations :
1. The system should be implemented as a web app using Angular and Java Spring boot as backend.
2. No database should be used. Data should be populated using mock data.
3. System should function as local web app. No cloud hosting or containerization.
4. Keep the system simple and to align to only the requirements detailed in Business Requirements. No extra features.

## Technical Considerations :
1. Use latest versions of Angular and Java Spring boot to the date.
2. UI to backend communication should be on REST endpoints.
3. Use Maven as build packaging tool.
4. Both UI asnd backend are to be hosted locally.
5. Apply only necessary Tests and focus on thorough integration tests.
6. Make sure after each feature development tests are built and they are running before moving to code next feature.

## Success Criteria :
1. System is able to come up with login page and user is able to login with valid credentials.
2. Editor and approver to have following default hsrd coded login credentials :
    Editor : 
        Username - editor
        Password - password
    Approver : 
        Username - approver
        Password - password
3. Funds Dashboard should load up with dummy data upto 25 dummy funds.
4. System uses nice slick fonts and the scrolling on dashboard should be smooth.
5. The Funds Details pop should open with nice animation.
6. Editor should be able to edit the fund details and save the changes.
7. Approver should be able to approve or reject the fund details.
8. Read Only user should be only allowed to view the fund details.
