function toggleVisibility(id) {
    const matlabTestFile = document.getElementById(id);
    const matlabTestCasesTable = document.getElementById("matlabTestCases-"+id);
    const plusSymbol = document.getElementById("plus-"+id);
    const minusSymbol = document.getElementById("minus-"+id);
    const duration = document.getElementById("duration-"+id);

    if (matlabTestCasesTable.style.display === "none"){
        matlabTestFile.colSpan = "2";
        matlabTestCasesTable.style.display = "inline";
        plusSymbol.style.display = "none";
        minusSymbol.style.display = "inline";
        duration.style.display = "none";
    } else {
        matlabTestFile.colSpan = "1";
        matlabTestCasesTable.style.display = "none";
        plusSymbol.style.display = "inline";
        minusSymbol.style.display = "none";
        duration.style.display = "table-cell";
    }
}

function stackTrace(id){
    const matlabTestCaseStack = document.getElementById("stack-"+id);
    const plusSymbol = document.getElementById("plus-"+id);
    const minusSymbol = document.getElementById("minus-"+id);

    if (matlabTestCaseStack.style.display === "none"){
        matlabTestCaseStack.style.display = "block";
        plusSymbol.style.display = "none";
        minusSymbol.style.display = "inline";
    } else {
        matlabTestCaseStack.style.display = "none";
        plusSymbol.style.display = "inline";
        minusSymbol.style.display = "none";
    }
}

let lastActiveButton = null;

function showTests(status){
    const activeButton = document.getElementById(status);

    if (activeButton.classList.contains('jenkins-button--active')) {
        activeButton.classList.remove('jenkins-button--active');
        activeButton.classList.remove('jenkins-!-background-color-primary');
        showAllTests();
        lastActiveButton = null;
        return;
    }

    if (lastActiveButton) {
        lastActiveButton.classList.remove('jenkins-button--active');
    }

    activeButton.classList.add('jenkins-button--active');
    lastActiveButton = activeButton;

    let allMatlabTestCaseRows = document.querySelectorAll('#matlabTestCasesTableBody tr');
    allMatlabTestCaseRows.forEach(function(row) {
        if (status === 'TOTAL' || row.classList.contains(status)) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });

    let matlabTestFileRows = document.querySelectorAll('#testResultsTable tr.test-file-row');
    matlabTestFileRows.forEach(function(row) {
        let allNestedRows = row.querySelector('tbody').querySelectorAll('tr');
        let visibleRows = Array.from(allNestedRows).filter(function(nestedRow) {
            return nestedRow.style.display === '';
        });
        row.style.display = visibleRows.length === 0 ? 'none' : '';
    });
}

function showAllTests() {
    let allMatlabTestCaseRows = document.querySelectorAll('#matlabTestCasesTableBody tr');
    allMatlabTestCaseRows.forEach(function(row) {
        row.style.display = '';
    });

    let matlabTestFileRows = document.querySelectorAll('#testResultsTable tr.test-file-row');
    matlabTestFileRows.forEach(function(row) {
        row.style.display = '';
    });
}

document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.matlab-filter-btn').forEach(function(btn) {
        btn.addEventListener('click', function() {
            showTests(btn.id);
        });
    });

    document.querySelectorAll('[id^="plus-"], [id^="minus-"]').forEach(function(el) {
        el.addEventListener('click', function() {
            const prefix = el.id.startsWith("plus-") ? 5 : 6;
            const id = el.id.substring(prefix);
            if (document.getElementById("stack-" + id)) {
                stackTrace(id);
            } else {
                toggleVisibility(id);
            }
        });
    });
});
