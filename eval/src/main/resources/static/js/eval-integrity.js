document.addEventListener('DOMContentLoaded', function () {
    // 이벤트 위임을 사용하여 동적으로 로드된 HTMX 콘텐츠에도 이벤트가 적용되도록 함
    document.body.addEventListener('click', function (e) {
        // 1. 평가 시작 모달 열기 (eval/periods/list.html)
        const startBtn = e.target.closest('.btn-start-validation');
        if (startBtn) {
            e.preventDefault();
            const periodId = startBtn.getAttribute('data-period-id');
            window.currentStartPeriodId = periodId;

            const modalElement = document.getElementById('startIntegrityModal');
            if (modalElement) {
                const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
                modal.show();

                document.getElementById('startIntegrityLoading').classList.remove('d-none');
                document.getElementById('startIntegrityResults').classList.add('d-none');
                document.getElementById('btnConfirmStart').classList.add('d-none');
                document.getElementById('startBlockedMessage').classList.add('d-none');

                // 캐시 버스팅
                fetch(`/eval/evaluators/validate?periodId=${periodId}&t=${new Date().getTime()}`)
                    .then(response => {
                        if (!response.ok) throw new Error('Network response was not ok');
                        return response.json();
                    })
                    .then(data => {
                        document.getElementById('startIntegrityLoading').classList.add('d-none');
                        document.getElementById('startIntegrityResults').classList.remove('d-none');

                        const summary = document.getElementById('startIntegritySummary');
                        const tbody = document.getElementById('startIntegrityTableBody');
                        const confirmBtn = document.getElementById('btnConfirmStart');
                        const blockedMsg = document.getElementById('startBlockedMessage');
                        tbody.innerHTML = '';

                        if (data.length === 0) {
                            summary.innerHTML = '<i class="bi bi-check-circle-fill me-2 text-success"></i>모든 평가 매핑이 정상입니다. 평가를 시작할 수 있습니다.';
                            summary.className = 'alert bg-success bg-opacity-10 border-success border-opacity-25 text-success mb-4';
                            confirmBtn.classList.remove('d-none', 'btn-warning');
                            confirmBtn.classList.add('btn-success');
                            confirmBtn.innerHTML = '<i class="bi bi-play-circle me-2"></i>평가 시작';
                            blockedMsg.classList.add('d-none');
                            const tr = document.createElement('tr');
                            tr.innerHTML = '<td colspan="5" class="text-center py-4 text-muted">발견된 이상 데이터가 없습니다.</td>';
                            tbody.appendChild(tr);
                        } else {
                            const errorCount = data.filter(d => d.severity === 'ERROR').length;
                            const warningCount = data.filter(d => d.severity === 'WARNING').length;
                            const infoCount = data.filter(d => d.severity === 'INFO').length;


                             if (errorCount > 0) {
                                summary.innerHTML = `<i class="bi bi-x-octagon-fill me-2"></i>중대한 오류 <strong>${errorCount}건</strong>이 발견되어 평가를 시작할 수 없습니다.` +
                                    (warningCount + infoCount > 0 ? ` (경고/정보: ${warningCount + infoCount}건)` : '');
                                summary.className = 'alert bg-danger bg-opacity-10 border-danger border-opacity-25 text-danger mb-4';
                                confirmBtn.classList.add('d-none');
                                blockedMsg.classList.remove('d-none');
                             } else {
                                const totalNonError = warningCount + infoCount;
                                if (warningCount > 0) {
                                    summary.innerHTML = `<i class="bi bi-exclamation-triangle-fill me-2"></i>주의 사항이 <strong>${warningCount}건</strong> 발견되었습니다 (정보: ${infoCount}건). 확인 후 평가를 시작할 수 있습니다.`;
                                    summary.className = 'alert bg-warning bg-opacity-10 border-warning border-opacity-25 text-warning mb-4';
                                    confirmBtn.innerHTML = '<i class="bi bi-exclamation-triangle me-2"></i>주의 사항 무시하고 시작';
                                } else {
                                    summary.innerHTML = `<i class="bi bi-info-circle-fill me-2"></i>참고 사항이 <strong>${infoCount}건</strong> 있습니다. 평가 시작에 지장이 없습니다.`;
                                    summary.className = 'alert bg-info bg-opacity-10 border-info border-opacity-25 text-info mb-4';
                                    confirmBtn.innerHTML = '<i class="bi bi-play-fill me-2"></i>평가 시작하기';
                                }
                                confirmBtn.classList.remove('d-none', 'btn-success');
                                confirmBtn.classList.add('btn-warning');
                                blockedMsg.classList.add('d-none');
                            }



                            data.forEach(item => {
                                const tr = document.createElement('tr');
                                 let severityBadge = '';
                                if (item.severity === 'ERROR') {
                                    severityBadge = '<span class="badge bg-danger bg-opacity-10 text-danger border border-danger border-opacity-25">ERROR</span>';
                                } else if (item.severity === 'INFO') {
                                    severityBadge = '<span class="badge bg-info bg-opacity-10 text-info border border-info border-opacity-25">INFO</span>';
                                } else {
                                    severityBadge = '<span class="badge bg-warning bg-opacity-10 text-warning border border-warning border-opacity-25">WARNING</span>';
                                }

                                tr.innerHTML = `
                                    <td>${severityBadge}</td>
                                    <td>
                                        <div class="fw-bold">${item.evaluateeName}</div>
                                        <div class="small text-muted">${item.evaluateeId}</div>
                                    </td>
                                    <td class="text-muted">${item.deptName}</td>
                                    <td><span class="badge bg-secondary bg-opacity-25 text-light">${item.anomalyType}</span></td>
                                    <td class="text-white">${item.description}</td>
                                `;
                                tbody.appendChild(tr);
                            });
                        }
                    })
                    .catch(error => {
                        console.error('Error fetching anomalies:', error);
                        document.getElementById('startIntegrityLoading').innerHTML = `
                            <div class="alert alert-danger bg-danger bg-opacity-10 border-danger border-opacity-25 text-danger m-0">
                                <i class="bi bi-x-circle-fill me-2"></i>검사 중 서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.
                            </div>
                        `;
                    });
            }
        }

        // 2. 정합성 검사 모달 열기 (eval/evaluators/list.html)
        const checkBtn = e.target.closest('.btn-check-integrity');
        if (checkBtn) {
            e.preventDefault();
            const periodId = checkBtn.getAttribute('data-period-id');
            const modalElement = document.getElementById('integrityCheckModal');
            if (modalElement && periodId) {
                const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
                modal.show();

                document.getElementById('integrityLoading').classList.remove('d-none');
                document.getElementById('integrityResults').classList.add('d-none');

                fetch(`/eval/evaluators/validate?periodId=${periodId}&t=${new Date().getTime()}`)
                    .then(response => {
                        if (!response.ok) throw new Error('Network response was not ok');
                        return response.json();
                    })
                    .then(data => {
                        document.getElementById('integrityLoading').classList.add('d-none');
                        document.getElementById('integrityResults').classList.remove('d-none');

                        const summary = document.getElementById('integritySummary');
                        const tbody = document.getElementById('integrityTableBody');
                        tbody.innerHTML = '';

                        if (data.length === 0) {
                            summary.innerHTML = '<i class="bi bi-check-circle-fill me-2 text-success"></i>모든 평가 매핑이 정상입니다. 오류가 발견되지 않았습니다.';
                            summary.className = 'alert bg-success bg-opacity-10 border-success border-opacity-25 text-success mb-4';
                            const tr = document.createElement('tr');
                            tr.innerHTML = '<td colspan="5" class="text-center py-4 text-muted">발견된 이상 데이터가 없습니다.</td>';
                            tbody.appendChild(tr);
                        } else {
                            const errorCount = data.filter(d => d.severity === 'ERROR').length;
                            const warningCount = data.filter(d => d.severity === 'WARNING').length;
                            const infoCount = data.filter(d => d.severity === 'INFO').length;

                            summary.innerHTML = `<i class="bi bi-exclamation-triangle-fill me-2"></i>총 <strong>${data.length}</strong>건의 이상 항목이 발견되었습니다. (오류: ${errorCount}건, 경고: ${warningCount}건, 정보: ${infoCount}건)`;
                            
                            if (errorCount > 0) {
                                summary.className = 'alert bg-danger bg-opacity-10 border-danger border-opacity-25 text-danger mb-4';
                            } else if (warningCount > 0) {
                                summary.className = 'alert bg-warning bg-opacity-10 border-warning border-opacity-25 text-warning mb-4';
                            } else {
                                summary.className = 'alert bg-info bg-opacity-10 border-info border-opacity-25 text-info mb-4';
                            }


                            data.forEach(item => {
                                const tr = document.createElement('tr');
                                 let severityBadge = '';
                                if (item.severity === 'ERROR') {
                                    severityBadge = '<span class="badge bg-danger bg-opacity-10 text-danger border border-danger border-opacity-25">ERROR</span>';
                                } else if (item.severity === 'INFO') {
                                    severityBadge = '<span class="badge bg-info bg-opacity-10 text-info border border-info border-opacity-25">INFO</span>';
                                } else {
                                    severityBadge = '<span class="badge bg-warning bg-opacity-10 text-warning border border-warning border-opacity-25">WARNING</span>';
                                }


                                tr.innerHTML = `
                                    <td>${severityBadge}</td>
                                    <td>
                                        <div class="fw-bold">${item.evaluateeName}</div>
                                        <div class="small text-muted">${item.evaluateeId}</div>
                                    </td>
                                    <td class="text-muted">${item.deptName}</td>
                                    <td><span class="badge bg-secondary bg-opacity-25 text-light">${item.anomalyType}</span></td>
                                    <td class="text-white">${item.description}</td>
                                `;
                                tbody.appendChild(tr);
                            });
                        }
                    })
                    .catch(error => {
                        console.error('Error fetching anomalies:', error);
                        document.getElementById('integrityLoading').innerHTML = `
                            <div class="alert alert-danger bg-danger bg-opacity-10 border-danger border-opacity-25 text-danger m-0">
                                <i class="bi bi-x-circle-fill me-2"></i>검사 중 서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.
                            </div>
                        `;
                    });
            }
        }
    });
});

// 시작 확인 버튼 로직 (전역 설정)
window.confirmStart = function () {
    if (!window.currentStartPeriodId) return;

    const btn = document.getElementById('btnConfirmStart');
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>처리 중...';
    }

    const form = document.getElementById('startForm-' + window.currentStartPeriodId);
    if (form) form.submit();
};
