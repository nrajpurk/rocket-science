(define (propositions-in formula)
 (cond ((symbol? formula) (list formula))
       ((boolean? formula) '())
       ((and (list? formula) (not (null? formula)))
	(case (first formula)
	 ((not) (if (= (length formula) 2)
		    (propositions-in (second formula))
		    (panic "Unrecognized formula")))
	 ((and) (reduce unionq (map propositions-in (rest formula)) '()))
	 ((or) (reduce unionq (map propositions-in (rest formula)) '()))
	 (else (panic "Unrecognized formula"))))
       (else (panic "Unrecognized formula"))))

(define (all-truth-assignments propositions)
 (if (null? propositions)
     '(())
     (let ((truth-assignments (all-truth-assignments (rest propositions))))
      (append (map (lambda (truth-assignment)
		    (cons (list (first propositions) #t) truth-assignment))
		   truth-assignments)
	      (map (lambda (truth-assignment)
		    (cons (list (first propositions) #f) truth-assignment))
		   truth-assignments)))))

(define (lookup-proposition proposition truth-assignment)
 (cond ((null? truth-assignment) (panic "Proposition not in truth assignment"))
       ((eq? proposition (first (first truth-assignment)))
	(second (first truth-assignment)))
       (else (lookup-proposition proposition (rest truth-assignment)))))

(define (boolean-evaluate formula truth-assignment)
 (cond ((symbol? formula) (lookup-proposition formula truth-assignment))
       ((boolean? formula) formula)
       ((and (list? formula) (not (null? formula)))
	(case (first formula)
	 ((not) (if (= (length formula) 2)
		    (not (boolean-evaluate (second formula) truth-assignment))
		    (panic "Unrecognized formula")))
	 ((and) (every (lambda (formula)
			(boolean-evaluate formula truth-assignment))
		       (rest formula)))
	 ((or) (some (lambda (formula)
		      (boolean-evaluate formula truth-assignment))
		     (rest formula)))
	 (else (panic "Unrecognized formula"))))
       (else (panic "Unrecognized formula"))))


(define (truth-table formula)
 (map (lambda (truth-assignment)
       (list truth-assignment (boolean-evaluate formula truth-assignment)))
      (all-truth-assignments (propositions-in formula))))

(define (truth-tables-match? p pp)
 (let ((I (all-truth-assignments (propositions-in p))))
  (equal?
   (map (lambda (truth-assignment)
	 (list truth-assignment
	       (boolean-evaluate p truth-assignment))) I)
   (map (lambda (truth-assignment)
	 (list truth-assignment
	       (boolean-evaluate pp truth-assignment))) I))))

(define (pattern-variable? p) (member p '(e e1 e2 e3)))

(define (pattern-list-variable? p) (member p '(e... e1... e2... e3...)))

(define (lookup-pattern-variable p bindings)
 (cond ((null? bindings) (panic "Unbound pattern variable"))
       ((eq? (first (first bindings)) p) (second (first bindings)))
       (else (lookup-pattern-variable p (rest bindings)))))

(define (match p e)
 (cond
  ((pattern-variable? p) (list (list p e)))
  ((pattern-list-variable? p)
   (panic "Not good"))
  ((and (list? p) (= (length p) 1) (pattern-list-variable? (first p)))
   (list (list (first p) e)))
  ((and (list? p) (not (null? p)))
   (if (and (list? e) (not (null? e)))
       (append (match (first p) (first e))
	       (match (rest p) (rest e)))
       (list #f)))
  ((equal? p e) '())
  (else (list #f))))

(define (instantiate p bindings)
 (cond ((pattern-variable? p)
	(lookup-pattern-variable p bindings))
       ((pattern-list-variable? p)
	(panic "Not good"))
       ((and (list? p) (= (length p) 1) (pattern-list-variable? (first p)))
	(lookup-pattern-variable (first p) bindings))
       ((and (list? p) (not (null? p)))
	(cons (instantiate (first p) bindings)
	      (instantiate (rest p) bindings)))
       (else p)))

(define (applicable? rule e)
 (not (memq #f (match (first rule) e))))

(define (first-matching-rule rules e)
 (cond ((null? rules) #f)
       ((applicable? (first rules) e) (first rules))
       (else (first-matching-rule (rest rules) e))))

(define (apply-rule rule e)
 (instantiate (third rule) (match (first rule) e)))

(define (apply-rules rules e)
 (let ((rule (first-matching-rule rules e)))
  (if rule
      (rewrite rules (apply-rule rule e))
      e)))

(define (rewrite rules e)
 (if (list? e)
     (apply-rules rules (map (lambda (e) (rewrite  rules e)) e))
     e))

(define *simple-rules*
 '(((not #t) -> #f)
   ((not #f) -> #t)
   ((not (not e)) -> e)
   ((and) -> #t)
   ((and e) -> e)
   ((or) -> #f)
   ((or e) -> e)
   ((and e #f) -> #f)
   ((and e #t) -> e)
   ((and #t e...) -> (and e...))
   ((and #f e...) -> #f)
   ((or e #t) -> #t)
   ((or e #f) -> e)
   ((or #f e...) -> (or e...))
   ((or #t e...) -> #t)
   ((and e1 e2 e3 e...) -> (and e1 (and e2 (and e3 e...))))
   ((or e1 e2 e3 e...) -> (or e1 (or e2 (or e3 e...))))))

(define *compact-rules*
 '(((or e1 (or e2 e...)) -> (or e1 e2 e...))
   ((and e1 (and e2 e...)) -> (and e1 e2 e...))))
 

(define (boolean-simplify phi)
 (rewrite *compact-rules*
	  (rewrite *simple-rules* phi)))